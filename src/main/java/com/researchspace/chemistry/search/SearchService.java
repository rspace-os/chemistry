package com.researchspace.chemistry.search;

import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
import com.researchspace.chemistry.convert.convertor.OpenBabelConvertor;
import com.researchspace.chemistry.util.CommandExecutor;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

  @Value("${search.file.dir}")
  private String outputDir;

  private static final String CHEM_FILE_FORMAT = "smi";

  private File chemicalsMaster;

  private File fastSearchChemicals;

  private File nonIndexedChemicals;

  private final CommandExecutor commandExecutor;

  private final ConvertService convertService;

  private final OpenBabelConvertor openBabelConvertor;

  @Autowired
  public SearchService(
      CommandExecutor commandExecutor,
      ConvertService convertService,
      OpenBabelConvertor openBabelConvertor) {
    this.commandExecutor = commandExecutor;
    this.convertService = convertService;
    this.openBabelConvertor = openBabelConvertor;
  }

  @PostConstruct
  public void initFiles() throws IOException {
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

  public void clearIndexFiles() throws IOException {
    LOGGER.info("clearing search indexes...");

    chemicalsMaster.delete();
    fastSearchChemicals.delete();
    nonIndexedChemicals.delete();
    initFiles();

    LOGGER.info("... done");
  }

  public void saveChemicalToFile(SaveDTO saveDTO) throws IOException {
    String smiles = getSmilesFromOpenBabel(saveDTO.chemical(), saveDTO.chemicalFormat());
    FileWriter fileWriter = new FileWriter(chemicalsMaster, true);
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      printWriter.println(smiles.strip() + " " + saveDTO.chemicalId());
      printWriter.flush();
      LOGGER.info("Wrote smiles {} to search file.", smiles);
    } catch (Exception e) {
      String chemicalPreview = StringUtils.abbreviate(saveDTO.chemical(), 50);
      LOGGER.error("Error while saving chemical {}", chemicalPreview, e);
    }
  }

  public List<String> searchNonIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", chemicalsMaster.getPath(), "-s" + searchTerm, "-o" + CHEM_FILE_FORMAT, "-xt");
    LOGGER.info(
        "Searching without index for {} in file: {}", searchTerm, chemicalsMaster.getPath());
    LOGGER.info("output:");
    return commandExecutor.executeCommand(builder);
  }

  public List<String> searchIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", fastSearchChemicals.getPath(), "-s" + searchTerm.strip(), "-osmi", "-xt");
    LOGGER.info("Searching with index for {} in file: {}", searchTerm, fastSearchChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  public List<String> searchChemsFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", fastSearchChemicals.getPath(), "-s" + searchTerm.strip(), "-osmi", "-xt");
    LOGGER.info("Searching with index for {} in file: {}", searchTerm, fastSearchChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  // Optional: not currently configured
  private void combineChemicalFiles() throws IOException {
    File out = new File(fastSearchChemicals.getPath());
    FileWriter fileWriter = new FileWriter(out, true);
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      try (BufferedReader reader = new BufferedReader(new FileReader(chemicalsMaster))) {
        String line;
        while ((line = reader.readLine()) != null) {
          printWriter.println(line);
        }
      }
      // clear contents of non-indexed file, since they've been moved
      new FileWriter(chemicalsMaster).close();
      LOGGER.info("Combined chemical files");
    } catch (Exception e) {
      LOGGER.error("Error while combining chemical files", e);
    }
  }

  // not currently configured
  private void createIndexFile()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", fastSearchChemicals.getPath(), "-O" + nonIndexedChemicals.getPath());
    commandExecutor.executeCommand(builder);
  }

  public List<String> search(SearchDTO search)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if (search.chemicalSearchTerm() != null && !search.chemicalSearchTerm().isEmpty()) {
      String smiles = getSmilesFromOpenBabel(search.chemicalSearchTerm(), search.chemicalFormat());
      List<String> hits = new ArrayList<>();
      //      hits.addAll(searchIndexedFile(smiles));
      //      hits.addAll(searchNonIndexedFile(smiles));
      hits.addAll(searchChemsFile(smiles));
      return hits.stream()
          .map(input -> input.contains(" ") ? input.substring(input.lastIndexOf(" ") + 1) : input)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  /***
   * Smiles notation for a given chemical can vary dependent on the parser. We want to ensure smiles are in the format
   * generated by OpenBabel, while also maintaining maximum compatibility for conversion of incoming chem -> smiles.
   * Therefore, input chemicals are first converted to smiles, then those smiles (which may have been generated by
   * OpenBabel or Indigo) are "converted" using OpenBabel so they're in the format expected by OpenBabel for saving/
   * searching.
   * @return smiles string generated by OpenBabel, or the initial smiles if the OpenBabel conversion isn't successful
   */
  private String getSmilesFromOpenBabel(String originalChem, String originalFormat) {
    //    ConvertDTO conversion = originaFormat
    String initialSmiles = convertService.convert(new ConvertDTO(originalChem, "smiles"));
    return openBabelConvertor
        .convert(new ConvertDTO(initialSmiles, "smiles", "smiles"))
        .orElse(initialSmiles);
  }

  /**
   * Adds a new chemical to the OpenBabel fast search index file by building the appropriate
   * command.
   *
   * @param smiles The SMILES notation of the chemical.
   * @throws IOException If there is an error with file handling.
   * @throws ExecutionException If command execution fails.
   * @throws InterruptedException If the execution is interrupted.
   * @throws TimeoutException If the execution exceeds the timeout.
   */
  public void addChemicalToIndexFile(String smiles)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", "-:\"" + smiles + "\"", "-ofs", "-O" + fastSearchChemicals.getPath(), "-u");
    LOGGER.info(
        "Adding chemical with SMILES '{}' to the fast search index file: {}",
        smiles,
        fastSearchChemicals.getPath());
    commandExecutor.executeCommand(builder);
  }

  public void indexChemicals() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    // convert non-indexed file to fs
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", chemicalsMaster.getPath(), "-O", fastSearchChemicals.getPath(), "-u");
    LOGGER.info("indexing chemicals from {} to {}", chemicalsMaster.getPath(), fastSearchChemicals.getPath());
    commandExecutor.executeCommand(builder);
  }
}
