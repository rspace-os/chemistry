package com.researchspace.chemistry.search;

import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
import com.researchspace.chemistry.convert.convertor.OpenBabelConvertor;
import com.researchspace.chemistry.util.CommandExecutor;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class SearchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

  @Value("${search.file.dir}")
  private String outputDir;

  private static final String CHEM_FILE_FORMAT = "smi";

  // Master smiles file containing all chemicals which have been saved to the service, along with
  // the id of the chemical they represent from the `rspace-web` database
  private File chemicalsMaster;

  // Indexed version of `chemicalsMaster` in the OpenBabel FastSearch format for better performance
  private File fastSearchChemicals;

  // Smiles which have been added to `chemicalsMaster` since the last indexing of chemicals to the
  // `fastSearchChemicals` file.
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

  public void clearFiles() throws IOException {
    LOGGER.info("clearing search indexes...");

    chemicalsMaster.delete();
    fastSearchChemicals.delete();
    nonIndexedChemicals.delete();
    initFiles();

    LOGGER.info("... done");
  }

  /**
   * Saves smiles string to both the `chemicalsMaster.smi`, and `nonIndexedChemicals.smi` files. The
   * chemical master file holds all which have been saved in the format "{smiles} {id}" e.g. "CCC
   * 123". The `fastSearchChemicals.fs` file is updated periodically from chemicalsMaster.smi. The
   * `nonIndexed.smi` file keeps track of the difference between `fastSearchChemicals.fs` and
   * `chemicalsMaster.smi` i.e. `nonIndexed.smi` holds any chemicals which have been saved since the
   * last re-indexing of files from `chemicalsMaster.smi` to `fastSearchChemicals.fs`.
   */
  public void saveChemicals(SaveDTO saveDTO) throws IOException {
    String smiles = getSmilesFromOpenBabel(saveDTO.chemical(), saveDTO.chemicalFormat());
    FileWriter chemMasterFile = new FileWriter(chemicalsMaster, true);
    FileWriter nonIndexedFile = new FileWriter(nonIndexedChemicals, true);

    writeChem(chemMasterFile, smiles, saveDTO.chemicalId());
    writeChem(nonIndexedFile, smiles, saveDTO.chemicalId());
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

  public List<String> searchNonIndexedFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(
        "obabel", nonIndexedChemicals.getPath(), "-s" + searchTerm, "-o" + CHEM_FILE_FORMAT, "-xt");
    LOGGER.info(
        "Searching without index for {} in file: {}", searchTerm, nonIndexedChemicals.getPath());
    LOGGER.info("output:");
    return commandExecutor.executeCommand(builder);
  }

  public List<String> searchFastSearchFile(String searchTerm)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    ProcessBuilder builder = new ProcessBuilder();
    // -al 10000000 is a not-well documented switch which sets the limit of fast search candidates
    // to 10m (default is 4000) to ensure all chemicals are searched
    builder.command(
        "obabel",
        fastSearchChemicals.getPath(),
        "-s" + searchTerm.strip(),
        "-osmi",
        "-xt",
        "-al 10000000");
    LOGGER.info(
        "Searching with index for {} in file: {}", searchTerm, fastSearchChemicals.getPath());
    return commandExecutor.executeCommand(builder);
  }

  /***
   * Searches for a given chemical smile using OpenBabel.
   * Search is performed against both the `fastSearchChemicals.fs` (indexed) and `nonIndexed.smi` (chems added since
   * previous indexing) files.
   * */
  public List<String> search(SearchDTO search)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    if (search.chemicalSearchTerm() != null && !search.chemicalSearchTerm().isEmpty()) {
      String smiles = getSmilesFromOpenBabel(search.chemicalSearchTerm(), search.chemicalFormat());
      Set<String> hits = new HashSet<>();
      hits.addAll(searchNonIndexedFile(smiles));
      hits.addAll(searchFastSearchFile(smiles));
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
    String initialSmiles = convertService.convert(new ConvertDTO(originalChem, "smiles"));
    return openBabelConvertor
        .convert(new ConvertDTO(initialSmiles, "smiles", "smiles"))
        .orElse(initialSmiles);
  }

  /***
   * Batch job to index chemicals using OpenBabel fast search format, and clear the contents of the nonIndexed.smi
   * file once they've been indexed.
   */
  @Scheduled(cron = "${search.index.cron}")
  public void indexChemicals()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    // update fast search file
    ProcessBuilder builder = new ProcessBuilder();
    builder.command("obabel", chemicalsMaster.getPath(), "-O", fastSearchChemicals.getPath(), "-u");
    LOGGER.info(
        "indexing chemicals from {} to {}",
        chemicalsMaster.getPath(),
        fastSearchChemicals.getPath());
    commandExecutor.executeCommand(builder);

    // clear the nonIndexChemicals file, entries from which have been indexed
    new PrintWriter(nonIndexedChemicals).close();
  }
}
