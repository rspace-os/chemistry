package com.researchspace.chemistry.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.researchspace.chemistry.convert.ChemistryException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
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
    ImageDTO imageDTO = new ImageDTO("CCC", format, "100", "100");
    byte[] image = imageGenerator.generateImage(imageDTO);
    assert image.length > 0;
  }

  @Test
  public void whenImageSizeProvided_thenImageSizeIsCorrect() throws Exception {
    int imageWidthAndHeight = 100;
    ImageDTO imageDTO =
        new ImageDTO(
            "CCC", "png", String.valueOf(imageWidthAndHeight), String.valueOf(imageWidthAndHeight));
    byte[] bytes = imageGenerator.generateImage(imageDTO);
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
    assertEquals(imageWidthAndHeight, image.getWidth());
    assertEquals(imageWidthAndHeight, image.getHeight());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNoSizeProvided_thenUseDefaultImageSize(String imageWidthAndHeight)
      throws Exception {
    int defaultWidthAndHeight = 500;
    ImageDTO imageDTO = new ImageDTO("CCC", "png", imageWidthAndHeight, imageWidthAndHeight);
    byte[] bytes = imageGenerator.generateImage(imageDTO);
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
    assertEquals(defaultWidthAndHeight, image.getWidth());
    assertEquals(defaultWidthAndHeight, image.getHeight());
  }

  @ParameterizedTest
  @ValueSource(strings = {"gif", "txt", "pdf"})
  public void whenInvalidImageFormat_thenThrowException(String format) {
    ImageDTO imageDTO = new ImageDTO("CCC", format, "100", "100");
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Unsupported image format: " + format, exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNullOrEmptyImageFormat_thenThrowException(String format) {
    ImageDTO imageDTO = new ImageDTO("CCC", format, "100", "100");
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Output format is empty", exception.getMessage());
  }
}
