# Chemistry Database with Bingo Cartridge

This directory contains everything needed to spin up a PostgreSQL database with the EPAM Bingo cartridge for chemical searches.

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- At least 2GB free disk space
- Ports 5432 and 8081 available

### Start the Database

```bash
cd db
docker-compose up -d
```

This will:
- Build a PostgreSQL 15 container with Bingo extension
- Create the `chemistry` database
- Install and configure the Bingo cartridge
- Insert sample chemical data for testing
- Start pgAdmin for database management

### Check Status

```bash
docker-compose ps
docker-compose logs postgres-bingo
```

### Stop the Database

```bash
docker-compose down
```

To remove all data:
```bash
docker-compose down -v
```

## 📊 Connection Details

### PostgreSQL Database
- **Host:** localhost
- **Port:** 5432  
- **Database:** chemistry
- **Username:** chemistry_user
- **Password:** chemistry_password

### pgAdmin Web Interface
- **URL:** http://localhost:8080
- **Email:** admin@chemistry.local
- **Password:** admin

## 🧪 Testing Bingo Functionality

Once the database is running, you can test Bingo searches:

```sql
-- Connect to the database
psql -h localhost -U chemistry_user -d chemistry

-- Test exact search
SELECT chemical_id, smiles 
FROM chemicals 
WHERE molecule @ ('CCO', 'exact') = 1;

-- Test substructure search  
SELECT chemical_id, smiles 
FROM chemicals 
WHERE molecule @ ('CC', 'sub') = 1;

-- Test similarity search
SELECT chemical_id, smiles, bingo.sim(molecule, 'CCO') as similarity
FROM chemicals 
WHERE bingo.sim(molecule, 'CCO') > 0.7;
```

## 🏗️ Architecture

### Files Structure
```
db/
├── Dockerfile                    # PostgreSQL + Bingo image
├── docker-compose.yml           # Service orchestration
├── init-scripts/
│   ├── 01-init-bingo.sql       # Install Bingo extension
│   ├── 02-create-schema.sql    # Create tables and indexes
│   └── 03-sample-data.sql      # Insert test data
└── README.md                   # This file
```

### Sample Data
The database includes these test molecules:
- Ethanol (CCO)
- Propane (CCC) 
- Propanol (CCCO)
- Isopropanol (CC(C)O)
- Benzene (C1=CC=CC=C1)
- Aspirin (CC(=O)OC1=CC=CC=C1C(=O)O)
- Caffeine (CN1C=NC2=C1C(=O)N(C(=O)N2C)C)
- Testosterone (CC12CCC3C(C1CCC2O)CCC4=CC(=O)CCC34C)

## ⚙️ Application Configuration

To use this database with the chemistry application, update `application.properties`:

```properties
# Use Bingo for both search engine and repository
search.engine=bingo
search.repository=bingo

# Database connection
spring.datasource.url=jdbc:postgresql://localhost:5432/chemistry
spring.datasource.username=chemistry_user
spring.datasource.password=chemistry_password
```

## 🔧 Troubleshooting

### Port Conflicts
If ports 5432 or 8080 are in use, modify `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Use different external port
```

### Bingo Extension Issues
Check the logs for Bingo installation:
```bash
docker-compose logs postgres-bingo | grep -i bingo
```

### Container Health
Monitor container health:
```bash
docker-compose ps
docker exec chemistry-postgres-bingo pg_isready -U chemistry_user -d chemistry
```

### Reset Database
To start fresh:
```bash
docker-compose down -v
docker-compose up -d
```

## 📚 Additional Resources

- [EPAM Bingo Documentation](https://lifescience.opensource.epam.com/bingo/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Compose Reference](https://docs.docker.com/compose/)

## 🎯 Performance Tips

- The Bingo molecular index (`bingo_idx`) provides fast chemical searches
- For production, consider tuning PostgreSQL settings in the Dockerfile
- Monitor query performance with `EXPLAIN ANALYZE`
- Use connection pooling for high-throughput applications