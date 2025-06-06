-- Insert sample chemical data for testing
-- This script adds some basic molecules to test the Bingo functionality

\echo 'Inserting sample chemical data...'

\c chemistry;

-- Insert some sample molecules for testing with Bingo
INSERT INTO chemicals (smiles, chemical_id, molecule) VALUES 
    ('CCO', 'ethanol_001', 'CCO'),
    ('CCC', 'propane_001', 'CCC'),
    ('CCCO', 'propanol_001', 'CCCO'),
    ('CC(C)O', 'isopropanol_001', 'CC(C)O'),
    ('C1=CC=CC=C1', 'benzene_001', 'C1=CC=CC=C1'),
    ('CC(=O)OC1=CC=CC=C1C(=O)O', 'aspirin_001', 'CC(=O)OC1=CC=CC=C1C(=O)O'),
    ('CN1C=NC2=C1C(=O)N(C(=O)N2C)C', 'caffeine_001', 'CN1C=NC2=C1C(=O)N(C(=O)N2C)C'),
    ('CC12CCC3C(C1CCC2O)CCC4=CC(=O)CCC34C', 'testosterone_001', 'CC12CCC3C(C1CCC2O)CCC4=CC(=O)CCC34C')
ON CONFLICT (chemical_id) DO NOTHING;

\echo 'Sample chemical data inserted successfully!'

-- Show the inserted data
SELECT chemical_id, smiles, created_at FROM chemicals ORDER BY created_at;

\echo 'Sample data loaded successfully! Testing Bingo functionality...'

-- Test exact search
\echo 'Testing exact search for ethanol (CCO)...'
SELECT chemical_id, smiles FROM chemicals WHERE molecule @ ('CCO', 'exact') = 1;

-- Test substructure search  
\echo 'Testing substructure search for CC...'
SELECT chemical_id, smiles FROM chemicals WHERE molecule @ ('CC', 'sub') = 1;

\echo 'Bingo functionality tests completed!'
\echo 'Database setup finished successfully!'
\echo '';
\echo 'Connection details:';
\echo '  Host: localhost';
\echo '  Port: 5432';
\echo '  Database: chemistry';
\echo '  Username: chemistry_user';
\echo '  Password: chemistry_password';
\echo '';
\echo 'pgAdmin available at: http://localhost:8080';
\echo '  Email: admin@chemistry.local';
\echo '  Password: admin';
\echo '';