-- Initialise Bingo extension and schema
CREATE EXTENSION IF NOT EXISTS bingo;

CREATE TABLE IF NOT EXISTS chemicals (
    id SERIAL PRIMARY KEY,
    smiles VARCHAR(2000) NOT NULL,
    chemical_id VARCHAR(255) NOT NULL UNIQUE,
    molecule TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
DROP INDEX IF EXISTS idx_chemicals_molecule_bingo;
DROP INDEX IF EXISTS idx_chemicals_chemical_id;
CREATE INDEX idx_chemicals_molecule_bingo ON chemicals USING bingo_idx (molecule bingo.molecule);
CREATE INDEX idx_chemicals_chemical_id ON chemicals(chemical_id);

-- Basic grants
GRANT ALL PRIVILEGES ON TABLE chemicals TO postgres;
GRANT USAGE, SELECT ON SEQUENCE chemicals_id_seq TO postgres;