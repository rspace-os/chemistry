"""Predicts bond dissociation enthalpies for a drawn structure.

The model returns a number for every bond, including kinds it never saw in training (ring bonds,
multiple bonds, rare element pairs). Those are reported as "not supported", never as a value.

A second, independently trained model checks each value. Where the two disagree by more than the
configured limit the bond is reported as low confidence, again without a value.
"""

import hashlib
import json
from decimal import ROUND_HALF_UP, Decimal
from importlib.metadata import version
from pathlib import Path

import torch
from chemprop import data, featurizers
from chemprop.models import MolAtomBondMPNN
from rdkit import Chem
from rdkit.Chem import rdDepictor
from rdkit.Chem.Draw import rdMolDraw2D

KCAL_TO_KJ = 4.184
MAX_HEAVY_ATOMS = 150
BOND_SYMBOLS = {
    Chem.BondType.SINGLE: "-",
    Chem.BondType.DOUBLE: "=",
    Chem.BondType.TRIPLE: "#",
    Chem.BondType.AROMATIC: ":",
}
WEAKEST_COLOUR = (1.0, 0.55, 0.55)
# The library's default for bond notes is a pale blue that is hard to read on white.
BOND_VALUE_COLOUR = (0.1, 0.2, 0.6)


class InvalidStructure(ValueError):
    pass


class BondStrengthPredictor:
    def __init__(self, model_dir: Path):
        self.metadata = json.loads((model_dir / "metadata.json").read_text())
        weights = model_dir / self.metadata["weightsFile"]
        self.weights_sha256 = hashlib.sha256(weights.read_bytes()).hexdigest()
        self.model = _load(weights)
        check = model_dir / self.metadata["crossCheck"]["weightsFile"]
        self.check_weights_sha256 = hashlib.sha256(check.read_bytes()).hexdigest()
        self.check_model = _load(check)
        self.max_difference = self.metadata["crossCheck"]["maxDifferenceKcalMol"]
        self.featurizer = featurizers.SimpleMoleculeMolGraphFeaturizer()
        self.elements = set(self.metadata["elements"])
        self.bond_types = set(self.metadata["bondTypes"])

    def method(self) -> dict:
        m = self.metadata
        return {
            "name": m["name"],
            "version": m["version"],
            "weightsSha256": self.weights_sha256,
            "quantity": m["quantity"],
            "trainingData": m["trainingData"],
            "referenceMethod": m["referenceMethod"],
            "meanAbsoluteErrorKcalMol": m["meanAbsoluteErrorKcalMol"],
            "accuracyNote": m["accuracyNote"],
            "supportedElements": m["elements"],
            "crossCheck": {
                "weightsSha256": self.check_weights_sha256,
                "maxDifferenceKcalMol": self.max_difference,
            },
            "libraries": {
                "chemprop": version("chemprop"),
                "rdkit": version("rdkit"),
                "torch": version("torch"),
            },
        }

    def analyse(self, molfile: str) -> dict:
        mol = Chem.MolFromMolBlock(molfile, removeHs=False)
        if mol is None or mol.GetNumAtoms() == 0:
            raise InvalidStructure("The structure could not be read as a molfile.")

        result = {
            "supported": True,
            "unsupportedReason": None,
            "unsupportedDetail": None,
            "bonds": [],
            "supportedBondCount": 0,
            "totalBondCount": 0,
            "imageSvg": None,
            "method": self.method(),
        }
        reason, detail = self._structure_problem(mol)
        if reason:
            result.update(supported=False, unsupportedReason=reason, unsupportedDetail=detail)
            return result

        with_hydrogens = _add_hydrogens(mol)
        predictions = self._predict(self.model, with_hydrogens)
        check = self._predict(self.check_model, with_hydrogens)
        bonds = self._heavy_atom_bonds(with_hydrogens, predictions, check)
        bonds += self._hydrogen_bonds(with_hydrogens, predictions, check)

        supported = [b for b in bonds if b["supported"]]
        if supported:
            min(supported, key=lambda b: b["bdeKcalMol"])["weakest"] = True
        result["bonds"] = bonds
        result["totalBondCount"] = sum(b["hydrogenCount"] or 1 for b in bonds)
        result["supportedBondCount"] = sum(b["hydrogenCount"] or 1 for b in supported)
        result["imageSvg"] = self._draw(mol, bonds)
        return result

    def _structure_problem(self, mol):
        symbols = {a.GetSymbol() for a in mol.GetAtoms()}
        uncovered = sorted(symbols - self.elements)
        if uncovered:
            return "ELEMENT_NOT_COVERED", ", ".join(uncovered)
        if len(Chem.GetMolFrags(mol)) > 1:
            return "MULTIPLE_FRAGMENTS", None
        if any(a.GetNumRadicalElectrons() for a in mol.GetAtoms()):
            return "RADICAL", None
        # Charge-separated neutral groups (nitro, N-oxide, azide) are in the training data; ions and
        # zwitterions are not.
        if Chem.GetFormalCharge(mol) != 0 or any(_is_lone_charge(a) for a in mol.GetAtoms()):
            return "CHARGED", None
        if mol.GetNumHeavyAtoms() > MAX_HEAVY_ATOMS:
            return "TOO_LARGE", str(MAX_HEAVY_ATOMS)
        return None, None

    def _predict(self, model, mol):
        """Returns one [enthalpy, free energy] pair in kcal/mol per bond, in RDKit bond order."""
        dataset = data.MolAtomBondDataset([data.MolAtomBondDatapoint(mol=mol)], self.featurizer)
        loader = data.build_dataloader(dataset, batch_size=1, shuffle=False, drop_last=False)
        with torch.inference_mode():
            for batch in loader:
                bmg, V_d, E_d, X_d, *_, constraints = batch
                _, _, bond_predictions = model(bmg, V_d, E_d, X_d, constraints)
                return bond_predictions.numpy()
        return []

    def _bond_problem(self, bond, prediction, check):
        if bond.IsInRing():
            return "RING_BOND"
        if bond.GetBondType() != Chem.BondType.SINGLE:
            return "MULTIPLE_BOND"
        pair = "-".join(sorted([bond.GetBeginAtom().GetSymbol(), bond.GetEndAtom().GetSymbol()]))
        if pair not in self.bond_types:
            return "BOND_TYPE_NOT_COVERED"
        if abs(float(prediction[0]) - float(check[0])) > self.max_difference:
            return "LOW_CONFIDENCE"
        return None

    def _row(self, label, atoms, elements, hydrogen_count, problem, prediction):
        row = {
            "label": label,
            "atoms": atoms,
            "elements": elements,
            "hydrogenCount": hydrogen_count,
            "supported": problem is None,
            "unsupportedReason": problem,
            "bdeKcalMol": None,
            "bdeKjMol": None,
            "bdfeKcalMol": None,
            "bdfeKjMol": None,
            "weakest": False,
        }
        if problem is None:
            # Not rounded here: whoever shows a value rounds it once. Rounding to one decimal first
            # and to a whole number later turns 84.46 into 85.
            bde, bdfe = float(prediction[0]), float(prediction[1])
            row.update(
                bdeKcalMol=bde,
                bdeKjMol=bde * KCAL_TO_KJ,
                bdfeKcalMol=bdfe,
                bdfeKjMol=bdfe * KCAL_TO_KJ,
            )
        return row

    def _heavy_atom_bonds(self, mol, predictions, check):
        rows = []
        for bond in mol.GetBonds():
            a, b = bond.GetBeginAtom(), bond.GetEndAtom()
            if a.GetAtomicNum() == 1 or b.GetAtomicNum() == 1:
                continue
            symbol = BOND_SYMBOLS.get(bond.GetBondType(), "-")
            rows.append(
                self._row(
                    f"{a.GetSymbol()}{a.GetIdx() + 1}{symbol}{b.GetSymbol()}{b.GetIdx() + 1}",
                    [a.GetIdx() + 1, b.GetIdx() + 1],
                    [a.GetSymbol(), b.GetSymbol()],
                    None,
                    self._bond_problem(bond, predictions[bond.GetIdx()], check[bond.GetIdx()]),
                    predictions[bond.GetIdx()],
                )
            )
        return rows

    def _hydrogen_bonds(self, mol, predictions, check):
        """One row per heavy atom that carries hydrogens. Hydrogens on the same atom get the same
        prediction, because the model only sees the 2D graph."""
        rows = []
        for atom in mol.GetAtoms():
            if atom.GetAtomicNum() == 1:
                continue
            bonds = [
                mol.GetBondBetweenAtoms(atom.GetIdx(), n.GetIdx())
                for n in atom.GetNeighbors()
                if n.GetAtomicNum() == 1
            ]
            if not bonds:
                continue
            mean = sum(predictions[b.GetIdx()] for b in bonds) / len(bonds)
            check_mean = sum(check[b.GetIdx()] for b in bonds) / len(bonds)
            rows.append(
                self._row(
                    f"{atom.GetSymbol()}{atom.GetIdx() + 1}-H",
                    [atom.GetIdx() + 1],
                    [atom.GetSymbol(), "H"],
                    len(bonds),
                    self._bond_problem(bonds[0], mean, check_mean),
                    mean,
                )
            )
        return rows

    def _draw(self, mol, rows):
        """Draws the structure as given, with atom numbers, values in kcal/mol on the supported
        bonds between heavy atoms, and the weakest bond highlighted."""
        drawing = Chem.Mol(mol)
        if drawing.GetNumConformers() == 0 or drawing.GetConformer().Is3D() or _has_no_layout(drawing):
            rdDepictor.Compute2DCoords(drawing)
        for atom in drawing.GetAtoms():
            if atom.GetAtomicNum() != 1:
                atom.SetProp("atomNote", str(atom.GetIdx() + 1))

        highlight_atoms, highlight_bonds = [], []
        for row in rows:
            if not row["supported"]:
                continue
            if row["hydrogenCount"] is None:
                bond = drawing.GetBondBetweenAtoms(row["atoms"][0] - 1, row["atoms"][1] - 1)
                bond.SetProp("bondNote", _whole_number(row["bdeKcalMol"]))
                if row["weakest"]:
                    highlight_bonds.append(bond.GetIdx())
            elif row["weakest"]:
                highlight_atoms.append(row["atoms"][0] - 1)

        canvas = rdMolDraw2D.MolDraw2DSVG(640, 480)
        options = canvas.drawOptions()
        options.annotationFontScale = 0.65
        options.setHighlightColour(WEAKEST_COLOUR)
        options.setBondNoteColour(BOND_VALUE_COLOUR)
        rdMolDraw2D.PrepareAndDrawMolecule(
            canvas, drawing, highlightAtoms=highlight_atoms, highlightBonds=highlight_bonds
        )
        canvas.FinishDrawing()
        return canvas.GetDrawingText()


def _load(weights: Path):
    model = MolAtomBondMPNN.load_from_file(weights, map_location="cpu")
    model.eval()
    return model


def _add_hydrogens(mol):
    """Hydrogens drawn as atoms get hybridisation S from RDKit, but the model was trained on added
    hydrogens, which have none. Left alone, a drawn hydrogen changes the predictions."""
    with_hydrogens = Chem.AddHs(mol)
    for atom in with_hydrogens.GetAtoms():
        if atom.GetAtomicNum() == 1:
            atom.SetHybridization(Chem.HybridizationType.UNSPECIFIED)
    return with_hydrogens


def _is_lone_charge(atom) -> bool:
    charge = atom.GetFormalCharge()
    return charge != 0 and not any(n.GetFormalCharge() * charge < 0 for n in atom.GetNeighbors())


def _whole_number(value: float) -> str:
    """Rounds half up, as the table in RSpace does with the same value, so the two agree."""
    return str(Decimal(str(value)).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def _has_no_layout(mol) -> bool:
    return not mol.GetConformer().GetPositions()[:, :2].any()
