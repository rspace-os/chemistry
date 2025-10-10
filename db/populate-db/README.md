# Bingo SMILES Loader

A Python script for downloading ChEMBL SMILES data and saving in batches to the chemistry service.

## Prerequisites

### Required Dependencies

```bash
pip install requests pandas
```

### Manual Dataset Placement
Manually download the dataset:
1. Visit [ChEMBL22 on Kaggle](https://www.kaggle.com/datasets/art3mis/chembl22)
2. Download the dataset
3. Place the file as `chembl22.txt` in the project directory
4. Run the script again

## Environment Setup
The easiest way to set up is to use a [virtual environment](https://docs.python.org/3/library/venv.html):

```bash
python -m venv smiles_env
source smiles_env/bin/activate  # On Windows: smiles_env\Scripts\activate
pip install requests pandas kaggle rdkit
python smiles_processor.py
```

## Basic Usage

Run the script with default settings:

```bash
python smiles_processor.py
```

This will:
- Look for existing `chembl22.txt` file locally
- If not found, attempt to download from Kaggle
- Process up to 100,000 SMILES strings
- Send them in batches of 1,000 to `http://localhost:8090/chemistry/saveBatch`

## File Format

The expected file format is tab-separated values:
```
SMILES_STRING    CHEMBL_ID
CC(C)CC1=CC=C(C=C1)C(C)C(=O)O    CHEMBL123456
CCO    CHEMBL654321
```

Only the SMILES string (first column) is required.