package com.researchspace.chemistry.search;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  final ExecutorService executorService = Executors.newSingleThreadExecutor();

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

  public void saveChemicalToFile(String chemical, String chemicalId) throws IOException {
    FileWriter fileWriter = new FileWriter(nonIndexedChemicals, true);
    try (PrintWriter printWriter = new PrintWriter(fileWriter)) {
      printWriter.println(chemical + " " + chemicalId);
      printWriter.flush();
      LOGGER.info("Wrote chemical {} to file.", chemical);
    } catch (Exception e) {
      LOGGER.error("Error while saving chemical {}", chemical, e);
    }
  }

  // TODO: sanitise searchTerm
  public List<String> searchNonIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", nonIndexedChemicals.getPath(), "-s" + searchTerm, "-o" + format, "-xt");
    LOGGER.info(
        "Searching without index for {} in file: {}", searchTerm, nonIndexedChemicals.getPath());
    LOGGER.info("FOUND:");
    return executeCommand(builder);
  }

  public List<String> searchIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", index.getPath(), "-s" + searchTerm, "-o" + format, "-xt");
    LOGGER.info("Searching with index for {} in file: {}", searchTerm, indexedChemicals.getPath());
    return executeCommand(builder);
  }

  private List<String> executeCommand(ProcessBuilder builder)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    LOGGER.info("Executing command: {}", builder.command());
    builder.directory(null); // uses current working directory
    Process process = builder.start();
    List<String> matches = new ArrayList<>();
    InputStreamConsumer streamConsumer =
        new InputStreamConsumer(process.getInputStream(), matches::add);
    Future<?> future = executorService.submit(streamConsumer);
    process.waitFor();
    future.get(10, TimeUnit.SECONDS);
    LOGGER.info("Found matches: {}", String.join(", ", matches));
    return matches;
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
    executeCommand(builder);
  }

  public List<String> search(String chemicalSearchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if (chemicalSearchTerm != null && !chemicalSearchTerm.isEmpty()) {
      List<String> hits = new ArrayList<>();
      hits.addAll(searchIndexedFile(chemicalSearchTerm));
      hits.addAll(searchNonIndexedFile(chemicalSearchTerm));
      return hits;
    }
    return Collections.emptyList();
  }

  /***
   * Perform an action on each line of an input stream
   */
  private record InputStreamConsumer(InputStream inputStream, Consumer<String> consumer)
      implements Runnable {

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }
  }
}
