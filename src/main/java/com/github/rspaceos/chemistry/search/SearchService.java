package com.github.rspaceos.chemistry.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

  private static final String format = "smi";

  private static final String OUTPUT_DIR = "src/main/resources/chemical_files/";
//  private static final String format = "inchi";

  File nonIndexedChemicals = new File(OUTPUT_DIR + "non-indexed-new-chemicals." + format);

  File indexedChemicals = new File(OUTPUT_DIR + "indexed-chemicals." + format);

  File index = new File(OUTPUT_DIR + "index.fs");

  ExecutorService executorService = Executors.newSingleThreadExecutor();

  public void executeCommand(){
    try {

      if(format.equals("smi")){
        saveChemicalToFile("c1ccccc1", "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. N");
        saveChemicalToFile("CC", "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. N");
        saveChemicalToFile("C=C", "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. N");
      } else {
        saveChemicalToFile("InChI=1S/C6H6/c1-2-4-6-5-3-1/h1-6H", "'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. N'");
        saveChemicalToFile("InChI=1S/C2H6/c1-2/h1-2H3", "'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. N'");
        saveChemicalToFile("InChI=1S/C2H4/c1-2/h1-2H2", "LoremN");
      }

      searchNonIndexedFile("C=C");
      combineChemicalFiles();
      createIndexFile();
      searchIndexedFile("C=C");
    } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  /***
   * Add the chemical string to the non-indexed chemicals file
   * @param chemical smiles/smarts format chemical
   * @param chemicalId id of RSChemElement entity from rspace-web database
   */
  public void saveChemicalToFile(String chemical, String chemicalId) throws IOException {
    FileWriter fileWriter = new FileWriter(nonIndexedChemicals, true);
    try(PrintWriter printWriter = new PrintWriter(fileWriter);){
      printWriter.println(chemical + " " + chemicalId);
      printWriter.flush();
      LOGGER.info("Wrote chemical {} to file.", chemical);
    } catch (Exception e){
      LOGGER.error("Error while saving chemical {}", chemical, e);
    }
  }

  /***
   * Search the non-indexed file for chemical matches
   * @param searchTerm smiles/smarts format chemical
   */
  public List<String> searchNonIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", nonIndexedChemicals.getPath(), "-s"+searchTerm, "-o" +format, "-xt");
    LOGGER.info("Searching without index for {} in file: {}", searchTerm, nonIndexedChemicals.getPath());
    LOGGER.info("FOUND:");
    return executeCommand(builder);
  }

  /***
   * Find partial match of the given search terms
   * @param searchTerm smiles/smarts format chemical
   */
  public List<String> searchIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", index.getPath(), "-s"+searchTerm, "-o" +format, "-xt");
    LOGGER.info("Searching with index for {} in file: {}", searchTerm, indexedChemicals.getPath());
    return executeCommand(builder);
  }

  private List<String> executeCommand(ProcessBuilder builder)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    LOGGER.info("Executing command: {}", builder.command());
    builder.directory(null); // uses current working directory
    Process process = builder.start();
    List<String> matches = new ArrayList<>();
    StreamGobbler streamGobbler =
        new StreamGobbler(process.getInputStream(), matches::add);
    Future<?> future = executorService.submit(streamGobbler);
    process.waitFor();
    future.get(10, TimeUnit.SECONDS);
    LOGGER.info("Found matches: {}", String.join(", ", matches));
    return matches;
  }


  private void combineChemicalFiles() throws IOException {
    File out = new File(indexedChemicals.getPath());
    FileWriter fileWriter = new FileWriter(out, true);
    try(PrintWriter printWriter = new PrintWriter(fileWriter);){
      try(BufferedReader reader = new BufferedReader(new FileReader(nonIndexedChemicals))){
        String line;
        while((line = reader.readLine()) != null) {
          printWriter.println(line);
        }
      }
      // clear contents of non-indexed file, since they've been moved
      new FileWriter(nonIndexedChemicals).close();
      LOGGER.info("Combined chemical files");
    } catch (Exception e){
      LOGGER.error("Error while combining chemical files", e);
    }
  }

  private void createIndexFile()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", indexedChemicals.getPath(), "-O"+index.getPath());
    executeCommand(builder);
  }

  public List<String> search(String chemicalSearchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if(chemicalSearchTerm != null && !chemicalSearchTerm.isEmpty()){
      List<String> hits = new ArrayList<>();
      hits.addAll(searchIndexedFile(chemicalSearchTerm));
      hits.addAll(searchNonIndexedFile(chemicalSearchTerm));
      return hits;
    }
    return Collections.emptyList();
  }


  private static class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines()
          .forEach(consumer);
    }
  }
}
