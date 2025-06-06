package com.researchspace.chemistry.search.repository;

import com.researchspace.chemistry.search.SearchType;
import com.researchspace.chemistry.util.CommandExecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "search.repository", havingValue = "openbabel", matchIfMissing = true)
@EnableScheduling
public class OpenBabelChemicalRepository implements ChemicalRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenBabelChemicalRepository.class);
  private static final String CHEM_FILE_FORMAT = "smi";

  @Value("${search.file.dir}")
  private String outputDir;

  private File chemicalsMaster;
  private File fastSearchChemicals;
  private File nonIndexedChemicals;

  private final CommandExecutor commandExecutor;

  @Autowired
  public OpenBabelChemicalRepository(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public void initialize() throws IOException {
    File dataDir = new File(outputDir);
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    chemicalsMaster = new File(outputDir + "/chemicalsMaster." + CHEM_FILE_FORMAT);
    chemicalsMaster.createNewFile();

    fastSearchChemicals = new File(outputDir + "/fastSearchChemicals.fs");
    fastSearchChemicals.createNewFile();

    nonIndexedChemicals = new File(outputDir + "/nonIndexedChemicals." + CHEM_FILE_FORMAT);
    nonIndexedChemicals.createNewFile();
  }

  @Override
  public void saveChemical(String smiles, String chemicalId) throws IOException {
    FileWriter chemMasterFile = new FileWriter(chemicalsMaster, true);
    writeChem(chemMasterFile, smiles, chemicalId);
    addToNonIndexed(smiles, chemicalId);
  }

  @Override
  public void clearAll() throws IOException {
    if (chemicalsMaster.exists()) {
      chemicalsMaster.delete();
    }
    if (fastSearchChemicals != null && fastSearchChemicals.exists()) {
      fastSearchChemicals.delete();
    }
    if (nonIndexedChemicals != null && nonIndexedChemicals.exists()) {
      nonIndexedChemicals.delete();
    }
    initialize();
  }

  @Override
  public List<String> search(String smiles, SearchType searchType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    Set<String> hits = new HashSet<>();
    hits.addAll(searchNonIndexedFile(smiles, searchType));
    hits.addAll(searchFastSearchFile(smiles, searchType));
    return hits.stream()
        .map(input -> input.contains(" ") ? input.substring(input.lastIndexOf(" ") + 1) : input)
        .collect(Collectors.toList());
  }

  @Scheduled(cron = "${search.index.cron}")
  public void indexChemicals()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", chemicalsMaster.getPath(), "-O", fastSearchChemicals.getPath(), "-u");
    LOGGER.info(
        "indexing chemicals from {} to {}",
        chemicalsMaster.getPath(),
        fastSearchChemicals.getPath());
    commandExecutor.executeCommand(builder);

    new PrintWriter(nonIndexedChemicals).close();
  }

  public void addToNonIndexed(String smiles, String chemicalId) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(nonIndexedChemicals, true))) {
      writer.println(smiles.strip() + " " + chemicalId);
    }
  }

  private List<String> searchNonIndexedFile(String searchTerm, SearchType searchType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel",
        nonIndexedChemicals.getPath(),
        "-o" + CHEM_FILE_FORMAT,
        "-xt",
        "-s" + searchTerm,
        calculateSearchType(searchType));
    LOGGER.info(
        "Searching without index for {} in file: {}", searchTerm, nonIndexedChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  private List<String> searchFastSearchFile(String searchTerm, SearchType searchType)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel",
        fastSearchChemicals.getPath(),
        "-al 10000000",
        "-osmi",
        "-xt",
        "-s" + searchTerm.strip(),
        calculateSearchType(searchType));
    LOGGER.info(
        "Searching with index for {} in file: {}", searchTerm, fastSearchChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  private String calculateSearchType(SearchType searchType) {
    if (searchType == null) {
      return "";
    }

    if (SearchType.EXACT.equals(searchType)) {
      return "exact";
    }

    return "";
  }

  private void writeChem(FileWriter fileWriter, String smiles, String id) {
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      printWriter.println(smiles.strip() + " " + id);
      printWriter.flush();
    } catch (Exception e) {
      String chemicalPreview = StringUtils.abbreviate(smiles, 50);
      LOGGER.error("Error while saving chemical {}", chemicalPreview, e);
    }
  }
}
