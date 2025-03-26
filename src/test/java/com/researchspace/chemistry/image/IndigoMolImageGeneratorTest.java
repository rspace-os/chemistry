package com.researchspace.chemistry.image;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.convert.ConvertService;
import com.researchspace.chemistry.image.generator.IndigoImageUtils;
import com.researchspace.chemistry.image.generator.IndigoMolImageGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndigoMolImageGeneratorTest {

  @Mock private IndigoImageUtils indigoImageUtils;

  @Mock private ConvertService convertService;

  @InjectMocks private IndigoMolImageGenerator indigoMolImageGenerator;

  @Test
  public void whenSuccessfulConversion_thenReturnResults() {
    ImageDTO imageDTO = new ImageDTO("inputData", "inputFormat", "outputFormat", "200", "200");
    ConvertDTO convertDTO = new ConvertDTO("inputData", "inputFormat", "mol");
    String convertedData = "convertedData";
    byte[] imageBytes = new byte[] {1, 2, 3};

    when(convertService.convert(convertDTO)).thenReturn(convertedData);
    when(indigoImageUtils.generateImage(any(ImageDTO.class))).thenReturn(imageBytes);

    byte[] result = indigoMolImageGenerator.generateImage(imageDTO);

    assertNotNull(result);
    assertArrayEquals(imageBytes, result);
    verify(convertService).convert(convertDTO);
    verify(indigoImageUtils).generateImage(any(ImageDTO.class));
  }

  @Test
  public void whenConversionFails_thenReturnEmptyByteArray() {
    ImageDTO imageDTO = new ImageDTO("inputData", "inputFormat", "outputFormat", "200", "200");
    ConvertDTO convertDTO = new ConvertDTO("inputData", "inputFormat", "mol");

    when(convertService.convert(convertDTO)).thenThrow(new ChemistryException("Conversion failed"));

    byte[] result = indigoMolImageGenerator.generateImage(imageDTO);

    assertNotNull(result);
    assertEquals(0, result.length);
  }
}
