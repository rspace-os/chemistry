package com.github.rspaceos.chemistry.convert;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ConvertServiceTest {
  private static final String SAMPLE_MOLECULE = "CCO"; // Ethanol in SMILES format
  private static final String SAMPLE_REACTION = "CCO>>CCC"; // Simple reaction in SMILES

  @Mock Convertor convertor;

  @InjectMocks private ConvertService convertService;

  @Test
  void convertFormat_ShouldConvertToSmiles() {
    ConvertDTO dto = new ConvertDTO(SAMPLE_MOLECULE, "smiles");
    String result = convertService.convertFormat(dto);
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void convertFormat_ShouldConvertToCDXML() {
    ConvertDTO dto = new ConvertDTO(SAMPLE_MOLECULE, "cdxml");
    String result = convertService.convertFormat(dto);
    assertNotNull(result);
    assertTrue(result.contains("CDXML"));
  }

  @Test
  void convertFormat_ShouldConvertToRXN() {
    ConvertDTO dto = new ConvertDTO(SAMPLE_REACTION, "rxn");
    String result = convertService.convertFormat(dto);
    assertNotNull(result);
    assertTrue(result.contains("$RXN"));
  }

  @Test
  void convertFormat_WithInvalidFormat_ShouldReturnEmptyString() {
    ConvertDTO dto = new ConvertDTO(SAMPLE_MOLECULE, "invalid");
    String result = convertService.convertFormat(dto);
    assertEquals("", result);
  }

  //  @Test
  //  void exportImage_ShouldExportPNG() {
  //    ConvertDTO dto = new ConvertDTO(SAMPLE_MOLECULE, "png");
  //    byte[] result = convertService.exportImage(dto);
  //    assertNotNull(result);
  //    assertTrue(result.length > 0);
  //  }
  //
  //  @Test
  //  void exportImage_ShouldExportJPG() {
  //    ConvertDTO dto = new ConvertDTO(SAMPLE_MOLECULE, "jpg");
  //    byte[] result = convertService.exportImage(dto);
  //    assertNotNull(result);
  //    assertTrue(result.length > 0);
  //  }
  //
  //  @Test
  //  void exportImage_ShouldHandleReactionInput() {
  //    ConvertDTO dto = new ConvertDTO(SAMPLE_REACTION, "png");
  //    byte[] result = convertService.exportImage(dto);
  //    assertNotNull(result);
  //    assertTrue(result.length > 0);
  //  }
}
