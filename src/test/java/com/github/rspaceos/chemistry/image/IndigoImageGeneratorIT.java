package com.github.rspaceos.chemistry.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.rspaceos.chemistry.convert.ChemistryException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IndigoImageGeneratorIT {

  @Autowired IndigoImageGenerator imageGenerator;

  @ParameterizedTest
  @ValueSource(strings = {"png", "svg", "jpg", "jpeg"})
  public void whenValidImageFormat_thenImageGenerated(String format) {
    ImageDTO imageDTO = new ImageDTO("CCC", format);
    byte[] image = imageGenerator.generateImage(imageDTO);
    assert image.length > 0;
  }

  @ParameterizedTest
  @ValueSource(strings = {"gif", "txt", "pdf"})
  public void whenInvalidImageFormat_thenThrowException(String format) {
    ImageDTO imageDTO = new ImageDTO("CCC", format);
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Unsupported image format: " + format, exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNullOrEmptyImageFormat_thenThrowException(String format) {
    ImageDTO imageDTO = new ImageDTO("CCC", format);
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Output format is empty", exception.getMessage());
  }
}
