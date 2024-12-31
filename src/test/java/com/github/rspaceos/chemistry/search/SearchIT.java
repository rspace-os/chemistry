package com.github.rspaceos.chemistry.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
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
  final File INDEXED = new File(tempDir.getPath() + "/indexed.smi");

  final File NON_INDEXED = new File(tempDir.getPath() + "/non-indexed.smi");

  final File INDEX = new File(tempDir.getPath() + "/index.fs");

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
    searchService.saveChemicalToFile("C", "1234");
    searchService.saveChemicalToFile("CCC", "5678");

    String fileContents = Files.readString(NON_INDEXED.toPath());
    assertEquals("C 1234\nCCC 5678\n", fileContents);
  }

  @Test
  public void whenSearchChemicalExists_thenIsFound() throws Exception {
    searchService.saveChemicalToFile("C", "1234");
    searchService.saveChemicalToFile("CCC", "5678");

    List<String> results = searchService.search("CCC");
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesSubstructure_thenIsFound() throws Exception {
    searchService.saveChemicalToFile("C", "123");
    searchService.saveChemicalToFile("CCC", "5678");

    List<String> results = searchService.search("CC");
    assertEquals(1, results.size());
    assertTrue(results.contains("5678"));
  }

  @Test
  public void whenSearchMatchesMultilpleSubstructures_thenAllReturned() throws Exception {
    searchService.saveChemicalToFile("C", "123");
    searchService.saveChemicalToFile("CCC", "5678");
    searchService.saveChemicalToFile("CCCC", "789");

    List<String> results = searchService.search("CC");
    assertEquals(2, results.size());
    assertTrue(results.contains("5678"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenMultipleHitsForSameStructure_thenAllHitIdsReturned() throws Exception {
    searchService.saveChemicalToFile("C", "123");
    searchService.saveChemicalToFile("CCC", "456");
    searchService.saveChemicalToFile("CCC", "789");

    List<String> results = searchService.search("CCC");
    assertEquals(2, results.size());
    assertTrue(results.contains("456"));
    assertTrue(results.contains("789"));
  }

  @Test
  public void whenNoMatches_thenEmptyListReturned() throws Exception {
    searchService.saveChemicalToFile("C", "123");
    searchService.saveChemicalToFile("CCC", "456");
    searchService.saveChemicalToFile("CCC", "789");

    List<String> results = searchService.search("CCO");
    assertEquals(0, results.size());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenEmptyOrNullSearchTerm_thenEmptyListReturned(String searchTerm) throws Exception {
    searchService.saveChemicalToFile("C", "123");
    searchService.saveChemicalToFile("CCC", "456");
    searchService.saveChemicalToFile("CCC", "789");

    List<String> results = searchService.search(searchTerm);
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
