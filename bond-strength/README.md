# Bond strength service

Proof of concept. Predicts the bond dissociation enthalpy of each bond in a structure. It runs
as its own container next to the chemistry service, which forwards requests to it from
`POST /chemistry/bondStrengths`. It makes no network calls at runtime.

## Run

With the chemistry service, from the repo root:

    docker compose -f docker-compose.yaml -f docker-compose.bond-strength.yaml up --build

The chemistry service finds it through `bondstrength.service.url` (environment variable
`BONDSTRENGTH_SERVICE_URL`). When that is empty, `/chemistry/bondStrengths` answers 503.

On its own, for development (Python 3.13):

    pip install -r requirements-dev.txt
    uvicorn app:app --port 8000
    python -m pytest

## API

`POST /predict` with `{"molfile": "..."}` returns:

- `supported`, and when false `unsupportedReason` (`ELEMENT_NOT_COVERED`, `MULTIPLE_FRAGMENTS`,
  `RADICAL`, `CHARGED`, `TOO_LARGE`) with `unsupportedDetail`. The chemistry service adds
  `REACTION`.
- `bonds`: one row per bond between heavy atoms, and one row per heavy atom that carries
  hydrogens (`hydrogenCount`). Atom numbers are 1-based and follow the molfile. A supported row
  has the enthalpy (`bde`) and free energy (`bdfe`) in kcal/mol and kJ/mol. An unsupported row has
  `unsupportedReason` (`RING_BOND`, `MULTIPLE_BOND`, `BOND_TYPE_NOT_COVERED`, `LOW_CONFIDENCE`) and
  no values.
- `imageSvg`: the structure with atom numbers, values in whole kcal/mol on the bonds between
  heavy atoms, and the weakest bond highlighted.
- `method`: model name and version, SHA-256 of the weights, training data, accuracy and library
  versions.

`GET /health` returns the same `method` block.

## Model

`model/best.pt` is a Chemprop bond-level model trained by ResearchSpace on
[BDE-db2](https://doi.org/10.6084/m9.figshare.19367051) (St. John et al., CC BY 4.0), using the
dataset's own train, validation and test split. `model/training-config.toml` is the training
configuration and `model/metadata.json` holds what the service reports about the model.

`model/check.pt` is a second, smaller model trained the same way (`model/check-training-config.toml`,
test error 1.22 kcal/mol). It is only used as a check: a bond where the two models differ by more
than 5 kcal/mol is reported as `LOW_CONFIDENCE` with no value. On the test set that hides 2% of
bonds and catches 15 of the 25 predictions that are wrong by more than 10 kcal/mol.

- Mean absolute error on the test set (1,000 molecules, 12,586 bonds): 0.80 kcal/mol against the
  DFT reference values (M06-2X/def2-TZVP). It has not been checked against experiment.
- Covered: single bonds outside rings, between C, H, N, O, S, F, Cl, P and I, for element pairs
  with at least 300 bonds in the training data (`bondTypes` in `metadata.json`).
- Not covered: ring bonds, multiple bonds, ions, zwitterions, radicals, other elements. Breaking
  one ring bond does not give two fragments, so the quantity is not defined there.

## Known limits

- The confidence check is coarse. Without it, 25 of the 12,586 test bonds are wrong by more than
  10 kcal/mol (the C-H bonds of unsubstituted benzene and pyridine come out 12 to 30 too low); it
  catches those two molecules and 15 of the 25, not all.
- Hydrogens on the same atom always get the same value, because the model sees the 2D graph only.
