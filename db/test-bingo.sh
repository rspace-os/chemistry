#!/bin/bash

# Test script for PostgreSQL with Bingo extension

set -euo pipefail

echo "Testing PostgreSQL database..."

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until docker exec bingo-postgres pg_isready -U postgres -d bingo > /dev/null 2>&1; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done

echo "PostgreSQL is up and running!"

# Check if Bingo extension is installed
echo "Checking if Bingo extension is installed..."
BINGO_INSTALLED=$(docker exec bingo-postgres psql -U postgres -d bingo -t -A -c "SELECT COUNT(*) FROM pg_extension WHERE extname = 'bingo';" | tr -d '\r' | xargs)

if [ "$BINGO_INSTALLED" = "0" ]; then
  echo "Bingo extension is not installed (unexpected for this image)."
  echo "Diagnostics:"
  docker exec bingo-postgres ls -la /usr/lib/postgresql/14/lib | grep -i bingo || true
  docker exec bingo-postgres ls -la /usr/share/postgresql/14/extension | grep -i bingo || true
  echo "Container logs may have details about CREATE EXTENSION failure."
  echo "Exiting with non-zero status."
  exit 1
else
  echo "Bingo extension is installed."

  # Insert a test chemical
  echo "Inserting test chemical..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "INSERT INTO chemicals (smiles, chemical_id, molecule) VALUES ('C1CCCCC1', 'cyclohexane', 'C1CCCCC1'::bingo.molecule) ON CONFLICT (chemical_id) DO NOTHING;"

  # Test exact structure search
  echo "Testing exact structure search..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', 'exact') = 1;"

  # Test substructure search
  echo "Testing substructure search..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CC', '')::bingo.sub;"

  echo "Tests completed with full Bingo functionality!"
fi
