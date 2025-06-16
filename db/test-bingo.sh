#!/bin/bash

# Test script for PostgreSQL with Bingo extension

echo "Testing PostgreSQL database..."

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until docker exec bingo-postgres pg_isready -U postgres > /dev/null 2>&1; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done

echo "PostgreSQL is up and running!"

# Check if Bingo extension is installed
echo "Checking if Bingo extension is installed..."
BINGO_INSTALLED=$(docker exec bingo-postgres psql -U postgres -d bingo -t -c "SELECT COUNT(*) FROM pg_extension WHERE extname = 'bingo';")

if [ "$BINGO_INSTALLED" -eq "0" ]; then
  echo "Bingo extension is not installed."
  echo "Please install the Bingo extension by following the instructions in the README.md file:"
  echo "1. Install required packages"
  echo "2. Clone and build Bingo"
  echo "3. Create the Bingo extension"
  echo "4. Update the molecule column type"
  echo "5. Create the Bingo molecular index"
  echo ""
  echo "For basic testing without Bingo extension:"

  # Insert a test chemical without Bingo functionality
  echo "Inserting test chemical (without Bingo functionality)..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "INSERT INTO chemicals (smiles, chemical_id, molecule) VALUES ('C1CCCCC1', 'cyclohexane', 'C1CCCCC1');"

  # Basic query without Bingo functionality
  echo "Testing basic query (without Bingo functionality)..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "SELECT * FROM chemicals WHERE chemical_id = 'cyclohexane';"

  echo "Tests completed (limited functionality without Bingo extension)."
else
  echo "Bingo extension is installed."

  # Insert a test chemical
  echo "Inserting test chemical..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "INSERT INTO chemicals (smiles, chemical_id, molecule) VALUES ('C1CCCCC1', 'cyclohexane', 'C1CCCCC1');"

  # Test exact structure search
  echo "Testing exact structure search..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', 'exact') = 1;"

  # Test substructure search
  echo "Testing substructure search..."
  docker exec bingo-postgres psql -U postgres -d bingo -c "SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CC', '')::bingo.sub;"

  echo "Tests completed with full Bingo functionality!"
fi
