-- Create initial schema for chemistry database
-- This script sets up the basic table structure with Bingo support

\echo 'Creating chemistry database schema...'

\c chemistry;

-- Check available types in bingo schema
\echo 'Checking available types in bingo schema...'
SELECT typname FROM pg_type WHERE typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'bingo');

-- Create the chemicals table with molecule column (Bingo will handle this as text)
CREATE TABLE IF NOT EXISTS chemicals (
    id SERIAL PRIMARY KEY,
    smiles VARCHAR(2000) NOT NULL,
    chemical_id VARCHAR(255) NOT NULL UNIQUE,
    molecule TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_chemicals_chemical_id ON chemicals(chemical_id);
CREATE INDEX IF NOT EXISTS idx_chemicals_created_at ON chemicals(created_at);

-- Create Bingo molecular index for fast chemical searches 
-- Check available operator classes first
\echo 'Available operator classes for bingo_idx:'
SELECT opcname FROM pg_opclass WHERE opcmethod = (SELECT oid FROM pg_am WHERE amname = 'bingo_idx');

-- Note: Bingo index creation will be handled separately
-- CREATE INDEX IF NOT EXISTS bingo_idx ON chemicals USING bingo_idx(molecule bmolecule);

-- Create a function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_chemicals_updated_at 
    BEFORE UPDATE ON chemicals 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

\echo 'Chemistry database schema created successfully!'

-- Show table structure
\d chemicals;

-- Show indexes
SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'chemicals';