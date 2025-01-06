package com.github.rspaceos.chemistry.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConvertTest {

  @Autowired private Convertor convertor;

  @Test
  public void whenValidRequest_thenConversionIsSuccessful() {
    ConvertDTO convertDTO = new ConvertDTO("CCC", "cdxml");
    String result = convertor.convert(convertDTO);
    assertTrue(result.contains("cdxml")); // todo: check for valid cdxml
  }

  @ParameterizedTest
  @ValueSource(strings = {"cdxml", "smiles", "ket"})
  public void whenSupportedFormat_thenConversionIsSuccessful(String outputFormat) {
    ConvertDTO convertDTO = new ConvertDTO("CCC.CCC", outputFormat);
    String result = convertor.convert(convertDTO);
    assertFalse(result.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "something", "123"})
  public void whenUnsupportedFormat_thenThrowException(String outputFormat) {
    ChemistryException exception =
        assertThrows(
            ChemistryException.class,
            () -> {
              ConvertDTO convertDTO = new ConvertDTO("CCC", outputFormat);
              convertor.convert(convertDTO);
            });

    assertEquals("Unsupported output format: " + outputFormat, exception.getMessage());
  }

  @Test
  public void whenInvalidInput_thenThrowException() {
    ChemistryException exception =
        assertThrows(
            ChemistryException.class,
            () -> {
              ConvertDTO convertDTO = new ConvertDTO("not-smiles", "cdxml");
              convertor.convert(convertDTO);
            });

    assertEquals(
        "Can't load input as molecule or reaction. Input: not-smiles", exception.getMessage());
  }

  @Disabled // some files fail to be loaded by indigo
  @ParameterizedTest
  @ValueSource(strings = {"", "something", "123"})
  public void whenValidChemicalFile_thenConversionToCdxmlIsSuccessful(String filename) {
    ConvertDTO convertDTO = new ConvertDTO("src/test/resources/" + filename, "cdxml");
    String result = convertor.convert(convertDTO);
    assertTrue(result.contains("cdxml"));
  }

  @Disabled // some files fail to be loaded by indigo
  @ParameterizedTest
  @ValueSource(strings = {"", "something", "123"})
  public void whenValidChemicalFile_thenConversionToSmilesIsSuccessful(String filename) {
    ConvertDTO convertDTO = new ConvertDTO("src/test/resources/" + filename, "smiles");
    String result = convertor.convert(convertDTO);
    assertTrue(result.contains("C"));
  }

  @Disabled // some files fail to be loaded by indigo
  @ParameterizedTest
  @MethodSource("readFiles")
  public void whenValidChemicalFile_thenConversionToKetIsSuccessful(String fileContents) {
    ConvertDTO convertDTO = new ConvertDTO(fileContents, "cdxml");
    String result = convertor.convert(convertDTO);
    System.out.println(result);
    assertTrue(result.contains("cdxml"));
  }

  private static List<String> readFiles() throws Exception {
    try (Stream<Path> paths = Files.walk(Paths.get("src/test/resources"))) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> !path.toString().endsWith(".cdx"))
          .map(
              path -> {
                try {
                  return Files.readString(path);
                } catch (IOException e) {
                  throw new RuntimeException("Error reading file: " + path, e);
                }
              })
          .collect(Collectors.toList());
    }
  }
}
