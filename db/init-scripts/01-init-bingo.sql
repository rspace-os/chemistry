-- Initialize Bingo extension and schema

-- Ensure database exists (usually created via POSTGRES_DB). Here we assume 'bingo' DB exists.
-- Create Bingo extension if not exists
CREATE EXTENSION IF NOT EXISTS bingo;

-- Create chemicals table if not exists, using bingo.molecule type
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables WHERE table_name='chemicals' AND table_schema='public'
  ) THEN
    CREATE TABLE chemicals (
        id SERIAL PRIMARY KEY,
        smiles VARCHAR(2000) NOT NULL,
        chemical_id VARCHAR(255) NOT NULL UNIQUE,
        molecule bingo.molecule,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
  END IF;
END $$;

-- If a pre-existing table had molecule as TEXT/VARCHAR, convert it
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns 
    WHERE table_name='chemicals' AND column_name='molecule' 
      AND udt_name IN ('text','varchar')
  ) THEN
    EXECUTE 'ALTER TABLE chemicals ALTER COLUMN molecule TYPE bingo.molecule USING molecule::bingo.molecule';
  END IF;
END $$;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_chemicals_molecule_bingo ON chemicals USING bingo_idx (molecule);
CREATE INDEX IF NOT EXISTS idx_chemicals_chemical_id ON chemicals(chemical_id);

-- Basic grants
GRANT ALL PRIVILEGES ON TABLE chemicals TO postgres;
GRANT USAGE, SELECT ON SEQUENCE chemicals_id_seq TO postgres;
