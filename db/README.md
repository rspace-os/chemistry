# PostgreSQL with Bingo Extension

This directory contains the necessary files to spin up a PostgreSQL database with the Bingo chemical cartridge installed.

## What is Bingo?

Bingo is a PostgreSQL cartridge for chemical structure storage and searching. It allows for:
- Exact structure search
- Substructure search
- Similarity search
- Molecular formula search
- and more

## How to Use

### Starting the Database

To start the PostgreSQL database:

```bash
cd db
docker-compose up -d
```

This will:
1. Start a PostgreSQL container
2. Initialize the database with the necessary schema for chemical storage

### Installing the Bingo Extension

The Bingo extension needs to be installed manually after the container is running:

```bash
# Install required packages in the container
docker exec -it bingo-postgres apt-get update
docker exec -it bingo-postgres apt-get install -y build-essential cmake libpq-dev postgresql-server-dev-14 git wget unzip

# Clone and build Bingo
docker exec -it bingo-postgres bash -c "cd /tmp && git clone https://github.com/epam/Indigo.git && cd Indigo && mkdir build && cd build && cmake .. -DBUILD_INDIGO=ON -DBUILD_BINGO=ON -DBUILD_BINGO_POSTGRES=ON -DBINGO_PG_VERSION=14 && make -j$(nproc) && make install"

# Create the Bingo extension in the database
docker exec -it bingo-postgres psql -U postgres -d bingo -c "CREATE EXTENSION IF NOT EXISTS bingo;"

# Update the molecule column to use bingo.molecule type
docker exec -it bingo-postgres psql -U postgres -d bingo -c "ALTER TABLE chemicals ALTER COLUMN molecule TYPE bingo.molecule USING molecule::bingo.molecule;"

# Create Bingo molecular index
docker exec -it bingo-postgres psql -U postgres -d bingo -c "CREATE INDEX IF NOT EXISTS idx_chemicals_molecule_bingo ON chemicals using bingo_idx (molecule);"
```

### Connecting to the Database

You can connect to the database using the following credentials:

- Host: localhost
- Port: 5433
- Database: bingo
- Username: postgres
- Password: postgres

Example using psql:

```bash
psql -h localhost -p 5433 -U postgres -d bingo
```

### Database Schema

The database is initialized with a `chemicals` table that has the following structure:

```sql
CREATE TABLE chemicals (
    id SERIAL PRIMARY KEY,
    smiles VARCHAR(2000) NOT NULL,
    chemical_id VARCHAR(255) NOT NULL UNIQUE,
    molecule bingo.molecule,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Example Queries

Here are some example queries you can run against the database:

#### Exact Structure Search
```sql
SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', 'exact') = 1;
```

#### Substructure Search
```sql
SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', '')::bingo.sub;
```

#### Similarity Search
```sql
SELECT chemical_id FROM chemicals WHERE bingo.sim(molecule, 'C1CCCCC1') > 0.8;
```

## Testing the Setup

A test script is provided to verify that the PostgreSQL container with the Bingo extension is working correctly:

```bash
cd db
docker-compose up -d  # Start the container if not already running
chmod +x test-bingo.sh  # Make sure the script is executable
./test-bingo.sh
```

This script will:
1. Wait for PostgreSQL to be ready
2. Verify that the Bingo extension is installed
3. Insert a test chemical (cyclohexane) into the database
4. Perform an exact structure search
5. Perform a substructure search

## Stopping the Database

To stop the database:

```bash
cd db
docker-compose down
```

To stop the database and remove all data:

```bash
cd db
docker-compose down -v
```
