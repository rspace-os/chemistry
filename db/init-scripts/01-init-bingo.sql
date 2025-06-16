-- Create chemicals table without Bingo extension for now
CREATE TABLE IF NOT EXISTS chemicals (
    id SERIAL PRIMARY KEY,
    smiles VARCHAR(2000) NOT NULL,
    chemical_id VARCHAR(255) NOT NULL UNIQUE,
    molecule TEXT, -- Using TEXT instead of bingo.molecule until extension is installed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on chemical_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_chemicals_chemical_id ON chemicals(chemical_id);

-- Grant permissions to postgres user
GRANT ALL PRIVILEGES ON TABLE chemicals TO postgres;
GRANT USAGE, SELECT ON SEQUENCE chemicals_id_seq TO postgres;

-- Create a function to check if Bingo is installed
CREATE OR REPLACE FUNCTION is_bingo_installed() RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM pg_extension WHERE extname = 'bingo'
    );
END;
$$ LANGUAGE plpgsql;
