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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConvertServiceIT {

  @Autowired ConvertService convertService;

  private static final String VALID_CDXML_START = "<?xml version=\"1.0\"";

  @Test
  public void whenValidRequest_thenConversionIsSuccessful() {
    ConvertDTO convertDTO = new ConvertDTO("CCC", "smi", "cdxml");
    String result = convertService.convert(convertDTO);
    assertTrue(result.startsWith(VALID_CDXML_START));
  }

  @ParameterizedTest
  @ValueSource(strings = {"cdxml", "smiles", "ket"})
  public void whenSupportedFormat_thenConversionIsSuccessful(String outputFormat) {
    ConvertDTO convertDTO = new ConvertDTO("CCC.CCC", "smiles", outputFormat);
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

    assertEquals("Unable to perform conversion to cdxml.", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("readFilesForSmilesConversion")
  public void whenValidChemicalFile_thenConversionToSmilesIsSuccessful(Conversion conversion) {
    System.out.println("converting file: " + conversion.fileName);
    String result = convertService.convert(conversion.convertDTO);
    assertTrue(result.contains("C"));
  }

  @ParameterizedTest
  @MethodSource("readFilesForCdxmlConversion")
  public void whenValidChemicalFile_thenConversionToCdxmlIsSuccessful(Conversion conversion) {
    System.out.println("converting file: " + conversion.fileName);
    String result = convertService.convert(conversion.convertDTO);
    assertTrue(result.contains(VALID_CDXML_START));
  }

  @ParameterizedTest
  @MethodSource("readFilesForKetConversion")
  public void whenValidChemicalFile_thenConversionToKetIsSuccessful(Conversion conversion) {
    System.out.println("converting file: " + conversion.fileName);
    String validKetcherStart = "\"root\":{\"nodes";
    String result = convertService.convert(conversion.convertDTO);
    assertTrue(result.contains(validKetcherStart));
  }

  private static List<Conversion> readFilesForCdxmlConversion() throws Exception {
    List<String> excludes =
        List.of(
            "4qwn_GitHub_muffintoad_protein-structure-prediction.pdb",
            "amineNotAmide_GitHub_chemaxon_jchem-examples.rxn");
    return readFiles("cdxml", excludes);
  }

  private static List<Conversion> readFilesForSmilesConversion() throws Exception {
    List<String> excludes =
        List.of(
            "4qwn_GitHub_muffintoad_protein-structure-prediction.pdb",
            "benzene_GitHub_SEDenmarkLab_molli.mol2",
            "amineNotAmide_GitHub_chemaxon_jchem-examples.rxn");
    return readFiles("smi", excludes);
  }

  private static List<Conversion> readFilesForKetConversion() throws Exception {
    List<String> excludes =
        List.of(
            "ala_phe_ala.pdb",
            "dendrobine_GitHub_SEDenmarkLab_molli.mol2",
            "4qwn_GitHub_muffintoad_protein-structure-prediction.pdb",
            "benzene_GitHub_SEDenmarkLab_molli.mol2",
            "adrenaline.mol2",
            "dimethyl_sulfone_GitHub_SEDenmarkLab_molli.mol2",
            "mjs_GitHub_AspirinCode_GENiPPI.pdb",
            "amineNotAmide_GitHub_chemaxon_jchem-examples.rxn");
    return readFiles("ket", excludes);
  }

  private static List<Conversion> readFiles(String outputFormat, List<String> excludes)
      throws Exception {
    try (Stream<Path> paths = Files.walk(Paths.get("src/test/resources/chemistry_file_examples"))) {
      return paths
          .filter(
              file ->
                  Files.isRegularFile(file) && !excludes.contains(file.getFileName().toString()))
          .map(
              path -> {
                try {
                  return new Conversion(
                      path.getFileName().toString(),
                      new ConvertDTO(
                          path.toString().endsWith(".cdx")
                              ? Base64.getEncoder().encodeToString(Files.readAllBytes(path))
                              : Files.readString(path),
                          FilenameUtils.getExtension(path.toString()),
                          outputFormat));
                } catch (IOException e) {
                  throw new RuntimeException("Error reading file: " + path, e);
                }
              })
          .collect(Collectors.toList());
    }
  }

  static class Conversion {
    String fileName;
    ConvertDTO convertDTO;

    public Conversion(String fileName, ConvertDTO convertDTO) {
      this.fileName = fileName;
      this.convertDTO = convertDTO;
    }
  }
}
