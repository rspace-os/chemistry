package com.researchspace.chemistry.search;

import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  @Value("${search.file.format}")
  private String format;

  @Value("${search.file.dir}")
  private String outputDir;

  private File nonIndexedChemicals;

  private File indexedChemicals;

  private File index;

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final CommandExecutor commandExecutor;

  private final ConvertService convertService;

  @Autowired
  public SearchService(CommandExecutor commandExecutor, ConvertService convertService) {
    this.commandExecutor = commandExecutor;
    this.convertService = convertService;
  }

  @PostConstruct
  public void initFiles() throws IOException {
    File dataDir = new File(outputDir);
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    nonIndexedChemicals = new File(outputDir + "/non-indexed." + format);
    nonIndexedChemicals.createNewFile();

    indexedChemicals = new File(outputDir + "/indexed." + format);
    indexedChemicals.createNewFile();

    index = new File(outputDir + "/index.fs");
    index.createNewFile();
  }

  public String clearIndexFiles() throws IOException {
    LOGGER.info("clearing search indexes...");

    nonIndexedChemicals.delete();
    indexedChemicals.delete();
    index.delete();
    initFiles();

    LOGGER.info("... done");
    return "Ok";
  }


  public void saveChemicalToFile(String chemical, String chemicalId) throws IOException {
    String smiles = convertService.convert(new ConvertDTO(chemical, "smiles"));
    FileWriter fileWriter = new FileWriter(nonIndexedChemicals, true);
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      printWriter.println(smiles + " " + chemicalId);
      printWriter.flush();
      LOGGER.info("Wrote smiles {} to search file.", smiles);
    } catch (Exception e) {
      String chemicalPreview = StringUtils.abbreviate(chemical, 50);
      LOGGER.error("Error while saving chemical {}", chemicalPreview, e);
    }
  }

  public List<String> searchNonIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", nonIndexedChemicals.getPath(), "-s" + searchTerm, "-o" + format, "-xt");
    LOGGER.info(
        "Searching without index for {} in file: {}", searchTerm, nonIndexedChemicals.getPath());
    LOGGER.info("FOUND:");
    return commandExecutor.executeCommand(builder);
  }

  public List<String> searchIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", index.getPath(), "-s" + searchTerm, "-o" + format, "-xt");
    LOGGER.info("Searching with index for {} in file: {}", searchTerm, indexedChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  // not currently configured
  private void combineChemicalFiles() throws IOException {
    File out = new File(indexedChemicals.getPath());
    FileWriter fileWriter = new FileWriter(out, true);
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      try (BufferedReader reader = new BufferedReader(new FileReader(nonIndexedChemicals))) {
        String line;
        while ((line = reader.readLine()) != null) {
          printWriter.println(line);
        }
      }
      // clear contents of non-indexed file, since they've been moved
      new FileWriter(nonIndexedChemicals).close();
      LOGGER.info("Combined chemical files");
    } catch (Exception e) {
      LOGGER.error("Error while combining chemical files", e);
    }
  }

  // not currently configured
  private void createIndexFile()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", indexedChemicals.getPath(), "-O" + index.getPath());
    commandExecutor.executeCommand(builder);
  }

  public List<String> search(String chemicalSearchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if (chemicalSearchTerm != null && !chemicalSearchTerm.isEmpty()) {
      List<String> hits = new ArrayList<>();
      hits.addAll(searchIndexedFile(chemicalSearchTerm));
      hits.addAll(searchNonIndexedFile(chemicalSearchTerm));
      return hits.stream()
          .map(input -> input.contains(" ") ? input.substring(input.lastIndexOf(" ") + 1) : input)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

}
