import pytest
from fastapi.testclient import TestClient
from rdkit import Chem

from app import app
from bde import _annotate, _whole_number


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def molfile(smiles: str) -> str:
    return Chem.MolToMolBlock(Chem.MolFromSmiles(smiles))


def molfile_with_drawn_hydrogens(smiles: str) -> str:
    """Keeps the hydrogens that the SMILES writes as atoms."""
    params = Chem.SmilesParserParams()
    params.removeHs = False
    return Chem.MolToMolBlock(Chem.MolFromSmiles(smiles, params))


def predict_molfile(client, molfile: str) -> dict:
    response = client.post("/predict", json={"molfile": molfile})
    assert response.status_code == 200
    return response.json()


def predict(client, smiles: str) -> dict:
    return predict_molfile(client, molfile(smiles))


def by_label(result: dict) -> dict:
    return {b["label"]: b for b in result["bonds"]}


def values(result: dict) -> dict:
    return {b["label"]: b["bdeKcalMol"] for b in result["bonds"]}


def test_ethanol_values_are_close_to_the_dft_reference(client):
    result = predict(client, "CCO")
    bonds = by_label(result)

    assert result["supported"] is True
    assert set(bonds) == {"C1-C2", "C2-O3", "C1-H", "C2-H", "O3-H"}
    assert bonds["C1-C2"]["bdeKcalMol"] == pytest.approx(87.3, abs=2)
    assert bonds["C2-O3"]["bdeKcalMol"] == pytest.approx(94.8, abs=2)
    assert bonds["O3-H"]["bdeKcalMol"] == pytest.approx(103.5, abs=2)
    assert bonds["C1-H"]["hydrogenCount"] == 3
    assert [b["label"] for b in result["bonds"] if b["weakest"]] == ["C1-C2"]
    assert result["supportedBondCount"] == result["totalBondCount"] == 8
    assert result["imageSvg"].startswith("<?xml")
    assert len(result["method"]["weightsSha256"]) == 64


def test_ring_and_multiple_bonds_get_a_reason_not_a_value(client):
    bonds = by_label(predict(client, "C=CCC1CCCCC1"))

    assert bonds["C1=C2"]["unsupportedReason"] == "MULTIPLE_BOND"
    assert bonds["C4-C5"]["unsupportedReason"] == "RING_BOND"
    assert bonds["C1=C2"]["bdeKcalMol"] is None
    assert bonds["C2-C3"]["supported"] is True


def test_bond_type_with_little_training_data_is_not_covered(client):
    bonds = by_label(predict(client, "CCBr"))

    assert bonds["C2-Br3"]["unsupportedReason"] == "BOND_TYPE_NOT_COVERED"
    assert bonds["C1-C2"]["supported"] is True


@pytest.mark.parametrize(
    "smiles, reason",
    [
        ("C[Si](C)(C)C", "ELEMENT_NOT_COVERED"),
        ("CC(=O)[O-]", "CHARGED"),
        ("[NH3+]CC(=O)[O-]", "CHARGED"),
        pytest.param("[CH2-][N+](C)(C)C", "CHARGED", id="ylide"),
        pytest.param("C[S+](C)[O-]", "CHARGED", id="sulfoxide drawn with charges"),
        ("CCO.CC", "MULTIPLE_FRAGMENTS"),
        ("C[CH2]", "RADICAL"),
        pytest.param("C" * 151, "TOO_LARGE", id="151 heavy atoms"),
    ],
)
def test_whole_structure_not_supported(client, smiles, reason):
    result = predict(client, smiles)

    assert result["supported"] is False
    assert result["unsupportedReason"] == reason
    assert result["bonds"] == []
    assert result["imageSvg"] is None


def test_value_the_second_model_disagrees_with_is_low_confidence(client):
    bonds = by_label(predict(client, "c1ccncc1"))

    assert bonds["C3-H"]["unsupportedReason"] == "LOW_CONFIDENCE"
    assert bonds["C3-H"]["bdeKcalMol"] is None


@pytest.mark.parametrize(
    "smiles",
    [
        pytest.param("C[N+](=O)[O-]", id="nitro"),
        pytest.param("C[N+](C)(C)[O-]", id="N-oxide"),
        pytest.param("CN=[N+]=[N-]", id="azide"),
        pytest.param("C" * 150, id="150 heavy atoms"),
    ],
)
def test_whole_structure_supported(client, smiles):
    assert predict(client, smiles)["supported"] is True


@pytest.mark.parametrize(
    "smiles, with_drawn_hydrogens",
    [
        pytest.param("CCO", "C(C(O[H])([H])[H])([H])([H])[H]", id="every hydrogen drawn"),
        pytest.param("CC(O)CC", "CC(O)(CC)[H]", id="one hydrogen drawn"),
    ],
)
def test_drawn_hydrogens_do_not_change_the_values(client, smiles, with_drawn_hydrogens):
    drawn = predict_molfile(client, molfile_with_drawn_hydrogens(with_drawn_hydrogens))

    assert values(drawn) == pytest.approx(values(predict(client, smiles)), abs=0.01)


@pytest.mark.parametrize(
    "value, shown", [(84.464, "84"), (84.5, "85"), (85.5, "86"), (87.46, "87"), (99.5, "100")]
)
def test_picture_rounds_half_up_like_the_table(value, shown):
    assert _whole_number(value) == shown


def picture(client, smiles: str):
    result = predict(client, smiles)
    return by_label(result), *_annotate(Chem.MolFromMolBlock(molfile(smiles)), result["bonds"])


def test_picture_numbers_and_values_match_the_table(client):
    bonds, drawing, highlight_atoms, highlight_bonds = picture(client, "CCO")

    def note(begin, end):
        return drawing.GetBondBetweenAtoms(begin - 1, end - 1).GetProp("bondNote")

    assert [a.GetProp("atomNote") for a in drawing.GetAtoms()] == ["1", "2", "3"]
    assert note(1, 2) == _whole_number(bonds["C1-C2"]["bdeKcalMol"])
    assert note(2, 3) == _whole_number(bonds["C2-O3"]["bdeKcalMol"])
    assert highlight_bonds == [drawing.GetBondBetweenAtoms(0, 1).GetIdx()]
    assert highlight_atoms == []


def test_picture_highlights_the_atom_when_the_weakest_bond_is_to_hydrogen(client):
    bonds, drawing, highlight_atoms, highlight_bonds = picture(client, "Cc1ccccc1")

    assert bonds["C1-H"]["weakest"] is True
    assert highlight_atoms == [0]
    assert highlight_bonds == []
    assert not drawing.GetBondBetweenAtoms(1, 2).HasProp("bondNote")


def test_values_are_reported_without_rounding(client):
    bond = by_label(predict(client, "CCO"))["C1-C2"]

    assert bond["bdeKcalMol"] != round(bond["bdeKcalMol"], 1)
    assert bond["bdeKjMol"] == pytest.approx(bond["bdeKcalMol"] * 4.184, abs=1e-9)


def test_unreadable_structure_is_rejected(client):
    response = client.post("/predict", json={"molfile": "not a molfile"})

    assert response.status_code == 422
