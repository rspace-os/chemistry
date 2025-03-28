package com.researchspace.chemistry.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(properties = {"search.file.format=smi"})
@ContextConfiguration(initializers = SearchIT.Initializer.class)
public class SearchIT {

  @TempDir static File tempDir;

  @Autowired SearchService searchService;
  final File INDEXED = new File(tempDir.getPath() + "/fastSearchChemicals.fs");

  final File NON_INDEXED = new File(tempDir.getPath() + "/nonIndexedChemicals.smi");

  final File INDEX = new File(tempDir.getPath() + "/chemicalsMaster.smi");

  @AfterEach
  public void clearFileContents() {
    Arrays.asList(INDEXED, NON_INDEXED, INDEX)
        .forEach(
            file -> {
              try {
                new PrintWriter(file).close();
              } catch (FileNotFoundException e) {
              }
            });
  }

  @Test
  public void searchFilesCreatedCorrectly() {
    List<File> expectedFiles = Arrays.asList(INDEXED, NON_INDEXED, INDEX);
    expectedFiles.forEach(file -> assertTrue(file.exists()));
  }

  @Test
  public void whenSaveChemical_thenAddedToFile() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "1234"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "5678"));

    String fileContents = Files.readString(NON_INDEXED.toPath());
    assertEquals("C 1234\nCCC 5678\n", fileContents);
  }

  @Test
  public void whenSearchChemicalExists_thenIsFound() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "1234"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "5678"));

    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesSubstructure_thenIsFound() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "123"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "5678"));

    List<String> results = searchService.search(createSearchDTO("CC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesMultipleSubstructures_thenAllReturned() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "123"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "5678"));
    searchService.saveChemicalToFile(new SaveDTO("CCCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CC"));
    assertEquals(2, results.size());
    assertTrue(results.contains("5678"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenMultipleHitsForSameStructure_thenAllHitIdsReturned() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "123"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "456"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(2, results.size());
    assertTrue(results.contains("456"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenNoMatches_thenEmptyListReturned() throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "123"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "456"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CCO"));
    assertEquals(0, results.size());
  }

  @Test
  public void testClearingSearchIndexes() throws Exception {
    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(0, results.size());

    searchService.saveChemicalToFile(new SaveDTO("CCC", "1234"));
    results = searchService.search(createSearchDTO("CC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));

    // clear
    searchService.clearIndexFiles();
    results = searchService.search(createSearchDTO("CCC"));
    assertEquals(0, results.size());

    // confirm working for newly index files again
    searchService.saveChemicalToFile(new SaveDTO("CCC", "5678"));
    results = searchService.search(createSearchDTO("CC"));
    assertEquals(1, results.size());
    assertFalse(results.contains("1234"));
    assertTrue(results.contains("5678"));
  }

  @ParameterizedTest
  @MethodSource("readSuccessfulSearchFiles")
  public void whenChemicalIsSavedForSearch_thenShouldBeFound(String fileName) throws Exception {
    System.out.println(fileName);
    String fileContents = chemistryFileContents(fileName);
    searchService.saveChemicalToFile(new SaveDTO(fileContents, "1234"));
    List<String> results =
        searchService.search(new SearchDTO(fileContents, FilenameUtils.getExtension(fileName)));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));
  }

  private static List<String> readSuccessfulSearchFiles() throws Exception {
    List<String> searchFiles =
        List.of(
            "aspirin_CompTox_DTXSID5020108.mol",
            "aspirin_PubChem_2244.smiles",
            "aspirin_PubChem_2244_2d.sdf",
            "aspirin_PubChem_2244_3d.sdf",
            "benzylpenicillin_GitHub_fusion809_artwork.mrv",
            "caffeine.sdf",
            "colchicine.mol",
            "cosyntropin_CompTox_DTXSID201014470.mol",
            "curamycin_A_CompTox_DTXSID40223473.mol",
            "cyclophosphamide_GitHub_fusion809_artwork.mrv",
            "esterification.mrv",
            "lapatinib_PubChem_208908.smiles",
            "lapatinib_PubChem_208908_2d.sdf",
            "lapatinib_PubChem_208908_3d.sdf",
            "propane.smiles",
            "SGroupExpanded_GitHub_chemaxon_jchem-examples.mrv");
    try (Stream<Path> paths = Files.walk(Paths.get("src/test/resources/chemistry_file_examples"))) {

      return paths
          .filter(
              file ->
                  Files.isRegularFile(file) && searchFiles.contains(file.getFileName().toString()))
          .map(file -> file.getFileName().toString())
          .collect(Collectors.toList());
    }
  }

  private static List<String> readAllFiles() throws Exception {
    try (Stream<Path> paths = Files.walk(Paths.get("src/test/resources/chemistry_file_examples"))) {

      return paths
          .filter(Files::isRegularFile)
          .map(file -> file.getFileName().toString())
          .collect(Collectors.toList());
    }
  }

  @Test
  public void whenNewMoleculeIsSaved_thenShouldBeFoundInFastSearchIndex() throws Exception {
    String smiles = "CCO";
    searchService.saveChemicalToFile(createSaveDTO(smiles));
    List<String> results = searchService.search(createSearchDTO(smiles));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));
  }

  private void addChemicalsToUnindexed(int start) throws Exception {
    List<String> chemFileName = readAllFiles();
    int failCount = 0;
    for (int i = start; i < start + chemFileName.size(); i++) {
      try {
        String fileName = chemFileName.get(i);
        String fileType = FilenameUtils.getExtension(fileName);
        String chemFileContents = chemistryFileContents(fileName);
        searchService.saveChemicalToFile(
            new SaveDTO(chemFileContents, String.valueOf(i), fileType));
      } catch (Exception e) {
        failCount++;
        System.out.println("Failed to save file: " + chemFileName.get(i - start));
      }
    }
    System.out.println("Failed to save " + failCount + " files");
  }

  @Test
  public void searchWithFastSearch() throws Exception {
    addChemicalsToUnindexed(0);
    runChemicalIndexing();
    addChemicalsToUnindexed(50);
    runChemicalIndexing();
    addChemicalsToUnindexed(100);
    List<String> results = searchService.search(createSearchDTO("C"));
    assertEquals(1, results.size());
    // batch job to create fast search
    // search fast search
  }

  private void runChemicalIndexing() {
    try {
      searchService.indexChemicals();
    } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private String chemistryFileContents(String fileName) throws IOException {
    return Files.readString(Path.of("src/test/resources/chemistry_file_examples/" + fileName));
  }

  private SearchDTO createSearchDTO(String searchTerm) {
    return new SearchDTO(searchTerm, "smiles");
  }

  private SaveDTO createSaveDTO(String smiles) {
    return new SaveDTO(smiles, "smiles");
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenEmptyOrNullSearchTerm_thenEmptyListReturned(String searchTerm) throws Exception {
    searchService.saveChemicalToFile(new SaveDTO("C", "123"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "456"));
    searchService.saveChemicalToFile(new SaveDTO("CCC", "789"));

    List<String> results = searchService.search(createSearchDTO(searchTerm));
    assertEquals(0, results.size());
  }

  // set the file directory property to the temp directory managed by junit
  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertyValues.of("search.file.dir=" + tempDir).applyTo(context);
    }
  }
}
