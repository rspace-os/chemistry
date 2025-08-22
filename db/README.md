# PostgreSQL with Bingo Extension

This directory contains the docker configuration to spin up PostgreSQL 14 with the EPAM Bingo chemical cartridge installed.

## What is Bingo?
Bingo is a PostgreSQL cartridge for chemical structure storage and searching.

## How it works (at a glance)
- The Dockerfile downloads and compiles the Bingo cartridge
- Initialisation SQL in init-scripts/ creates the Bingo extension and chemicals table
- docker-compose builds and runs this image.

## Prerequisites
- Docker and Docker Compose

## Start the database
```bash
cd db
# Build and run
docker compose up -d
```

This will:
1. Build a custom image with Bingo installed
2. Start a PostgreSQL container on port 5433
3. Initialise the database, create the Bingo extension, and set up the schema

## Connect to the database

- Host: localhost
- Port: 5433
- Database: bingo
- Username: postgres
- Password: postgres

Example using psql:
```bash
psql -h localhost -p 5433 -U postgres -d bingo
```

## Database schema

The initialization ensures a `chemicals` table exists (or is migrated) with:
```sql
CREATE TABLE chemicals (
    id SERIAL PRIMARY KEY,
    smiles VARCHAR(2000) NOT NULL,
    chemical_id VARCHAR(255) NOT NULL UNIQUE,
    molecule TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
Indexes:
- `idx_chemicals_molecule_bingo` using `bingo_idx`
- `idx_chemicals_chemical_id`

## Example search queries

Exact structure search:
```sql
SELECT smiles, chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', '') ::bingo.exact;
```

Substructure search:
```sql
SELECT smiles, chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', '')::bingo.sub;
```

Similarity:
```sql
SELECT smiles, chemical_id FROM chemicals WHERE molecule @ (0.8, 1, 'CCO', 'Tanimoto')::bingo.sim;
```

## Stop the database

```bash
cd db
docker compose down
```

Remove all data:
```bash
cd db
docker compose down -v
```
