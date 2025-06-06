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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {"search.file.format=smi"})
@ContextConfiguration(initializers = SearchServiceIT.Initializer.class)
@TestPropertySource(properties = "search.repository=openbabel")
public class SearchServiceIT {

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
    searchService.saveChemicals(new SaveDTO("C", "1234"));
    searchService.saveChemicals(new SaveDTO("CCC", "5678"));

    String fileContents = Files.readString(NON_INDEXED.toPath());
    assertEquals("C 1234\nCCC 5678\n", fileContents);
  }

  @Test
  public void whenSearchChemicalExists_thenIsFound() throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "1234"));
    searchService.saveChemicals(new SaveDTO("CCC", "5678"));

    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesSubstructure_thenIsFound() throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "123"));
    searchService.saveChemicals(new SaveDTO("CCC", "5678"));

    List<String> results = searchService.search(createSearchDTO("CC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesMultipleSubstructures_thenAllReturned() throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "123"));
    searchService.saveChemicals(new SaveDTO("CCC", "456"));
    searchService.saveChemicals(new SaveDTO("CCCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CC"));
    assertEquals(2, results.size());
    assertTrue(results.contains("456"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenMultipleHitsForSameStructure_thenAllHitIdsReturned() throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "123"));
    searchService.saveChemicals(new SaveDTO("CCC", "456"));
    searchService.saveChemicals(new SaveDTO("CCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(2, results.size());
    assertTrue(results.contains("456"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenNoMatches_thenEmptyListReturned() throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "123"));
    searchService.saveChemicals(new SaveDTO("CCC", "456"));
    searchService.saveChemicals(new SaveDTO("CCC", "789"));

    List<String> results = searchService.search(createSearchDTO("CCO"));
    assertEquals(0, results.size());
  }

  @Test
  public void testClearingSearchIndexes() throws Exception {
    List<String> results = searchService.search(createSearchDTO("CCC"));
    assertEquals(0, results.size());

    searchService.saveChemicals(new SaveDTO("CCC", "1234"));
    results = searchService.search(createSearchDTO("CC"));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));

    // clear
    searchService.clearFiles();
    results = searchService.search(createSearchDTO("CCC"));
    assertEquals(0, results.size());

    // confirm working for newly index files again
    searchService.saveChemicals(new SaveDTO("CCC", "5678"));
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
    searchService.saveChemicals(new SaveDTO(fileContents, "1234"));
    List<String> results =
        searchService.search(new SearchDTO(fileContents, FilenameUtils.getExtension(fileName)));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));
  }

  @ParameterizedTest
  @MethodSource("readSuccessfulSearchFiles")
  public void whenChemicalIsSavedForSearch_thenFoundByExactMatch(String fileName) throws Exception {
    String fileContents = chemistryFileContents(fileName);
    searchService.saveChemicals(new SaveDTO(fileContents, "1234"));
    List<String> results =
        searchService.search(
            new SearchDTO(fileContents, FilenameUtils.getExtension(fileName), SearchType.EXACT));
    assertEquals(1, results.size());
    assertTrue(results.contains("1234"));
  }

  @Test
  public void whenExactMatchSearching_thenSubstructuresNotFound() throws Exception {
    add10ChemicalsFromIndex(0);
    List<String> substructureResults =
        searchService.search(new SearchDTO("CC", "smiles", SearchType.SUBSTRUCTURE));
    assertEquals(5, substructureResults.size());

    List<String> exactMatchResults =
        searchService.search(new SearchDTO("CC", "smiles", SearchType.EXACT));
    assertEquals(1, exactMatchResults.size());
  }

  @Test
  public void whenChemicalsNotIndexed_thenOnlyExactMatchesFound() throws Exception {
    add10ChemicalsFromIndex(0);
    List<String> results = searchService.search(new SearchDTO("CC", "smiles", SearchType.EXACT));
    assertEquals(1, results.size());
  }

  @Test
  public void whenChemicalsIndexed_thenOnlyExactMatchesFound() throws Exception {
    add10ChemicalsFromIndex(0);
    searchService.indexChemicals();
    List<String> results = searchService.search(new SearchDTO("CC", "smiles", SearchType.EXACT));
    assertEquals(1, results.size());
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

  private void add10ChemicalsFromIndex(int fromIndex) throws Exception {
    List<String> smiles =
        Files.readAllLines(Path.of("src/test/resources/search_file/chemicals.smi"));
    List<String> subset = smiles.subList(fromIndex, fromIndex + 10);
    int failCount = 0;
    for (int i = 0; i < 10; i++) {
      try {
        searchService.saveChemicals(
            new SaveDTO(subset.get(i), String.valueOf(fromIndex + i), "smi"));
      } catch (Exception e) {
        failCount++;
        System.out.println("Failed to save file: " + subset.get(i));
      }
    }
    System.out.println("Failed to save " + failCount + " files");
  }

  @Test
  public void whenNewlyAddedChemicalsIndexed_thenCorrectResultsFound() throws Exception {
    add10ChemicalsFromIndex(0);
    searchService.indexChemicals();
    List<String> results = searchService.search(createSearchDTO("c"));
    assertEquals(8, results.size());
    add10ChemicalsFromIndex(10);
    searchService.indexChemicals();
    List<String> results20 = searchService.search(createSearchDTO("c"));
    assertEquals(17, results20.size());
    add10ChemicalsFromIndex(20);
    searchService.indexChemicals();
    List<String> results30 = searchService.search(createSearchDTO("c"));
    assertEquals(26, results30.size());
    add10ChemicalsFromIndex(30);
    searchService.indexChemicals();
    List<String> results40 = searchService.search(createSearchDTO("c"));
    assertEquals(33, results40.size());
    add10ChemicalsFromIndex(40);
    searchService.indexChemicals();
    List<String> results50 = searchService.search(createSearchDTO("c"));
    assertEquals(40, results50.size());
  }

  @Test
  public void whenChemsIndexed_thenChemsFound() throws Exception {
    add10ChemicalsFromIndex(0);
    searchService.indexChemicals();
    List<String> results = searchService.search(createSearchDTO("C"));
    List<String> expectedChemIdHits = List.of("0", "1", "2", "4", "5", "6", "7", "8");
    assertEquals(expectedChemIdHits, results);
  }

  @Test
  public void whenChemsNotIndexed_thenChemsFound() throws Exception {
    add10ChemicalsFromIndex(0);
    List<String> results = searchService.search(createSearchDTO("C"));
    List<String> expectedChemIdHits = List.of("0", "1", "2", "4", "5", "6", "7", "8");
    assertEquals(expectedChemIdHits, results);
  }

  @Test
  public void whenFastSearchUpdated_thenNewlyIndexedChemsRemovedFromNonIndexedFile()
      throws Exception {
    add10ChemicalsFromIndex(0);
    assertEquals(10, Files.readAllLines(NON_INDEXED.toPath()).size());
    searchService.indexChemicals();
    assertEquals(0, Files.readAllLines(NON_INDEXED.toPath()).size());
  }

  @Test
  public void whenSameChemicalSavedTwice_thenBothAreFoundByExactMatchSearch() throws Exception {
    searchService.saveChemicals(new SaveDTO("CCC", "123"));
    searchService.saveChemicals(new SaveDTO("CC", "456"));
    searchService.saveChemicals(new SaveDTO("CCC", "789"));

    SearchDTO exactSearch = new SearchDTO("CCC", SearchType.EXACT);
    List<String> results = searchService.search(exactSearch);

    assertEquals(2, results.size());
    assertTrue(results.contains("123"));
    assertTrue(results.contains("789"));
  }

  private String chemistryFileContents(String fileName) throws IOException {
    return Files.readString(Path.of("src/test/resources/chemistry_file_examples/" + fileName));
  }

  private SearchDTO createSearchDTO(String searchTerm) {
    return new SearchDTO(searchTerm, "smiles");
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenEmptyOrNullSearchTerm_thenEmptyListReturned(String searchTerm) throws Exception {
    searchService.saveChemicals(new SaveDTO("C", "123"));
    searchService.saveChemicals(new SaveDTO("CCC", "456"));
    searchService.saveChemicals(new SaveDTO("CCC", "789"));

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
