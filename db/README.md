# PostgreSQL with Bingo Extension

This directory contains a clean, reproducible setup to spin up PostgreSQL 14 with the EPAM Bingo chemical cartridge installed and ready to use. The Bingo extension is built from source in a multi-stage Docker build to avoid architecture/ABI mismatches.

## What is Bingo?

Bingo is a PostgreSQL cartridge for chemical structure storage and searching. It allows for:
- Exact structure search
- Substructure search
- Similarity search
- Molecular formula search
- and more

## How it works (at a glance)
- A multi-stage Dockerfile compiles Indigo/Bingo against PostgreSQL 14 headers.
- Only the built artifacts (.so, .control, .sql) are copied into a lean postgres:14 runtime image.
- Initialization SQL in init-scripts/ creates the Bingo extension, ensures the chemicals table uses bingo.molecule, and adds indexes.
- docker-compose builds and runs this image.

## Prerequisites
- Docker and Docker Compose
- If you’re on Apple Silicon and encounter qemu issues, set `platform: linux/amd64` in docker-compose (comment included in file).

## Start the database

```bash
cd db
# Build and run
docker compose up -d
```

This will:
1. Build a custom image with Bingo installed
2. Start a PostgreSQL container on port 5433
3. Initialize the database, create the Bingo extension, and set up the schema

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
    molecule bingo.molecule,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
Indexes:
- `idx_chemicals_molecule_bingo` using `bingo_idx`
- `idx_chemicals_chemical_id`

## Example queries

Exact structure search:
```sql
SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', 'exact') = 1;
```

Substructure search:
```sql
SELECT chemical_id FROM chemicals WHERE molecule @ ('C1CCCCC1', '')::bingo.sub;
```

Similarity (if supported in your build):
```sql
SELECT chemical_id FROM chemicals WHERE bingo.sim(molecule, 'C1CCCCC1') > 0.8;
```

## Test the setup

A script is provided to verify the container:
```bash
cd db
docker compose up -d  # Start container if not already running
chmod +x test-bingo.sh
./test-bingo.sh
```
The script will:
1. Wait for PostgreSQL to be ready
2. Verify that the Bingo extension is installed
3. Insert a test chemical (cyclohexane)
4. Run exact and substructure searches

## Troubleshooting
- If `CREATE EXTENSION bingo` fails: ensure the image was built (not the plain postgres image). Run `docker compose build --no-cache` then `docker compose up -d`.
- If you changed init scripts but the schema didn’t update: Postgres only runs init scripts on first init of the data directory. Remove the volume and try again: `docker compose down -v && docker compose up -d`.
- On Apple Silicon problems: uncomment `platform: linux/amd64` in docker-compose and rebuild. Alternatively, build with `docker buildx` for your platform.

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
