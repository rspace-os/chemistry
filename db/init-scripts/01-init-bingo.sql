-- Initialize Bingo for chemistry database
-- This script runs the Bingo installation SQL directly

\echo 'Setting up Bingo...'

-- Connect to the chemistry database
\c chemistry;

-- Run the Bingo installation script
\i /usr/share/postgresql/15/extension/bingo--1.29.0.sql

-- Alternative verification - check if bingo schema exists
\dn bingo

\echo 'Bingo installed successfully!'