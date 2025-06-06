package com.researchspace.chemistry.search.repository;

import com.researchspace.chemistry.search.SearchType;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "search.repository", havingValue = "bingo")
@ConditionalOnClass(JdbcTemplate.class)
public class BingoChemicalRepository implements ChemicalRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(BingoChemicalRepository.class);

  private final JdbcTemplate jdbcTemplate;

  public BingoChemicalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void saveChemical(String smiles, String chemicalId) throws IOException {
    try {
      String sql =
          "INSERT INTO chemicals (smiles, chemical_id, molecule) VALUES (?, ?, ?) "
              + "ON CONFLICT (chemical_id) DO UPDATE SET "
              + "smiles = EXCLUDED.smiles, molecule = EXCLUDED.molecule";
      jdbcTemplate.update(sql, smiles, chemicalId, smiles);
      LOGGER.debug("Saved chemical with ID: {} to Bingo repository", chemicalId);
    } catch (Exception e) {
      throw new IOException("Failed to save chemical to Bingo repository: " + chemicalId, e);
    }
  }

  @Override
  public void clearAll() throws IOException {
    try {
      String sql = "DELETE FROM chemicals";
      int deletedRows = jdbcTemplate.update(sql);
      LOGGER.info("Cleared {} chemicals from Bingo repository", deletedRows);
    } catch (Exception e) {
      throw new IOException("Failed to clear chemicals from Bingo repository", e);
    }
  }

  @Override
  public void initialize() throws IOException {
    try {
      // Create Bingo extension if not exists
//      jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS bingo");

      // Create table with Bingo molecule column
      String createTableSql =
          """
          CREATE TABLE IF NOT EXISTS chemicals (
              id SERIAL PRIMARY KEY,
              smiles VARCHAR(2000) NOT NULL,
              chemical_id VARCHAR(255) NOT NULL UNIQUE,
              molecule bingo.molecule,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """;
      jdbcTemplate.execute(createTableSql);

      // Create index on chemical_id for faster lookups
      String createIndexSql =
          "CREATE INDEX IF NOT EXISTS idx_chemicals_chemical_id ON chemicals(chemical_id)";
      jdbcTemplate.execute(createIndexSql);

      // Create Bingo molecular index for fast chemical searches
      String createBingoIndexSql =
          "CREATE INDEX IF NOT EXISTS idx_chemicals_molecule_bingo ON chemicals using bingo_idx (molecule bingo.molecule);";
      jdbcTemplate.execute(createBingoIndexSql);

      LOGGER.info("Bingo chemical repository initialized with molecular indexing");
    } catch (Exception e) {
      throw new IOException("Failed to initialize Bingo chemical repository", e);
    }
  }

  public List<String> searchExact(String smiles) throws IOException {
    try {
      String sql = "SELECT chemical_id FROM chemicals WHERE molecule @ (?, 'exact') = 1";
      return jdbcTemplate.queryForList(sql, String.class, smiles);
    } catch (Exception e) {
      throw new IOException("Failed to perform exact search in Bingo repository", e);
    }
  }

  public List<String> searchSubstructure(String smiles) throws IOException {
    try {
      String sql = "SELECT chemical_id FROM chemicals WHERE molecule @ (?, '')::bingo.sub";
      return jdbcTemplate.queryForList(sql, String.class, smiles);
    } catch (Exception e) {
      throw new IOException("Failed to perform substructure search in Bingo repository", e);
    }
  }

  public List<String> searchSimilarity(String smiles, double threshold) throws IOException {
    try {
      String sql = "SELECT chemical_id FROM chemicals WHERE bingo.sim(molecule, ?) > ?";
      return jdbcTemplate.queryForList(sql, String.class, smiles, threshold);
    } catch (Exception e) {
      throw new IOException("Failed to perform similarity search in Bingo repository", e);
    }
  }

  @Override
  public List<String> search(String smiles, SearchType searchType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    LOGGER.debug("Performing {} search for: {}", searchType, smiles);

    if (searchType == SearchType.EXACT) {
      return searchExact(smiles);
    } else {
      return searchSubstructure(smiles);
    }
  }
}
