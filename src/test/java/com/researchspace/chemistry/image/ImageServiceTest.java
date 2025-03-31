package com.researchspace.chemistry.image;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.image.generator.IndigoImageGenerator;
import com.researchspace.chemistry.image.generator.OpenBabelImageGenerator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTest {

  @Mock private IndigoImageGenerator indigoImageGenerator;

  @Mock private OpenBabelImageGenerator openBabelImageGenerator;

  @InjectMocks private ImageService imageService;

  @Test
  void useIndigoFirst() {
    ImageDTO imageDTO = new ImageDTO("CCC", "smi", "png", "100", "100");
    byte[] image = new byte[] {1, 2, 3};
    when(indigoImageGenerator.generateImage(imageDTO)).thenReturn(Optional.of(image));

    byte[] result = imageService.exportImage(imageDTO);

    verify(indigoImageGenerator).generateImage(imageDTO);
    verify(openBabelImageGenerator, times(0)).generateImage(imageDTO);
    assertEquals(image, result);
  }

  @Test
  void whenIndigoFails_thenUseOpenBabel() {
    ImageDTO imageDTO = new ImageDTO("CCC", "smi", "png", "100", "100");
    byte[] image = new byte[] {1, 2, 3};
    when(indigoImageGenerator.generateImage(imageDTO)).thenReturn(Optional.empty());
    when(openBabelImageGenerator.generateImage(imageDTO)).thenReturn(Optional.of(image));

    byte[] result = imageService.exportImage(imageDTO);

    verify(indigoImageGenerator).generateImage(imageDTO);
    verify(openBabelImageGenerator).generateImage(imageDTO);
    assertEquals(image, result);
  }

  @Test
  void whenImageCannotBeGenerated_thenThrowException() {
    ImageDTO imageDTO = new ImageDTO("CCC", "smi", "png", "100", "100");
    when(indigoImageGenerator.generateImage(imageDTO)).thenReturn(Optional.empty());
    when(openBabelImageGenerator.generateImage(imageDTO)).thenReturn(Optional.empty());

    assertThrows(ChemistryException.class, () -> imageService.exportImage(imageDTO));

    verify(indigoImageGenerator).generateImage(imageDTO);
    verify(openBabelImageGenerator).generateImage(imageDTO);
  }
}
