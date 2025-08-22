-- Initialize Bingo extension and schema

CREATE EXTENSION IF NOT EXISTS bingo;

-- Create chemicals table if not exists, using TEXT type for molecule storage
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables WHERE table_name='chemicals' AND table_schema='public'
  ) THEN
    CREATE TABLE chemicals (
        id SERIAL PRIMARY KEY,
        smiles VARCHAR(2000) NOT NULL,
        chemical_id VARCHAR(255) NOT NULL UNIQUE,
        molecule TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
  END IF;
END $$;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_chemicals_molecule_bingo ON chemicals USING bingo_idx (molecule);
CREATE INDEX IF NOT EXISTS idx_chemicals_chemical_id ON chemicals(chemical_id);

-- Basic grants
GRANT ALL PRIVILEGES ON TABLE chemicals TO postgres;
GRANT USAGE, SELECT ON SEQUENCE chemicals_id_seq TO postgres;
