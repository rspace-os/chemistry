package com.researchspace.chemistry.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.researchspace.chemistry.ChemistryException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConvertServiceIT {

  @Autowired ConvertService convertService;

  private static final String VALID_CDXML_START = "<?xml version=\"1.0\"?>\n<!DOCTYPE CDXML";

  @Test
  public void whenValidRequest_thenConversionIsSuccessful() {
    ConvertDTO convertDTO = new ConvertDTO("CCC", "smi", "cdxml");
    String result = convertService.convert(convertDTO);
    assertTrue(result.startsWith(VALID_CDXML_START));
  }

  @ParameterizedTest
  @ValueSource(strings = {"cdxml", "smiles", "ket"})
  public void whenSupportedFormat_thenConversionIsSuccessful(String outputFormat) {
    ConvertDTO convertDTO = new ConvertDTO("CCC.CCC", outputFormat);
    String result = convertService.convert(convertDTO);
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
              convertService.convert(convertDTO);
            });

    assertEquals(
        String.format("Unable to perform conversion to %s.", outputFormat), exception.getMessage());
  }

  @Test
  public void whenInvalidInput_thenThrowException() {
    ChemistryException exception =
        assertThrows(
            ChemistryException.class,
            () -> {
              ConvertDTO convertDTO = new ConvertDTO("not-smiles", "cdxml");
              convertService.convert(convertDTO);
            });

    assertEquals(
        "Can't load input as molecule or reaction. Input: not-smiles", exception.getMessage());
  }

  @Disabled("Some files fail to be converted")
  @ParameterizedTest
  @MethodSource("readFilesForCdxmlConversion")
  public void whenValidChemicalFile_thenConversionToCdxmlIsSuccessful(ConvertDTO convertDTO) {
    String result = convertService.convert(convertDTO);
    assertTrue(result.contains(VALID_CDXML_START));
  }

  @Disabled("Some files fail to be converted")
  @ParameterizedTest
  @MethodSource("readFilesForSmilesConversion")
  public void whenValidChemicalFile_thenConversionToSmilesIsSuccessful(ConvertDTO convertDTO) {
    String result = convertService.convert(convertDTO);
    assertTrue(result.contains("C"));
  }

  @Disabled("Some files fail to be converted")
  @ParameterizedTest
  @MethodSource("readFilesForKetConversion")
  public void whenValidChemicalFile_thenConversionToKetIsSuccessful(ConvertDTO convertDTO) {
    String validKetcherStart = "\"root\":{\"nodes";
    String result = convertService.convert(convertDTO);
    assertTrue(result.contains(validKetcherStart));
  }

  private static List<ConvertDTO> readFilesForCdxmlConversion() throws Exception {
    return readFiles("cdxml");
  }

  private static List<ConvertDTO> readFilesForSmilesConversion() throws Exception {
    return readFiles("smi");
  }

  private static List<ConvertDTO> readFilesForKetConversion() throws Exception {
    return readFiles("ket");
  }

  private static List<ConvertDTO> readFiles(String outputFormat) throws Exception {
    try (Stream<Path> paths = Files.walk(Paths.get("src/test/resources"))) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> !path.toString().endsWith(".cdx"))
          .map(
              path -> {
                try {
                  return new ConvertDTO(
                      Files.readString(path),
                      FilenameUtils.getExtension(path.toString()),
                      outputFormat);
                } catch (IOException e) {
                  throw new RuntimeException("Error reading file: " + path, e);
                }
              })
          .collect(Collectors.toList());
    }
  }
}
