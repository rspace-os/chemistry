package com.researchspace.chemistry.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.image.generator.ImageGeneratorChain;
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
public class ImageGeneratorIT {

  static final String INPUT_FORMAT = "smi";

  @Autowired ImageGeneratorChain imageGenerator;

  @ParameterizedTest
  @ValueSource(strings = {"png", "svg", "jpg", "jpeg"})
  public void whenValidImageFormat_thenImageGenerated(String outputFormat) {
    ImageDTO imageDTO = new ImageDTO("CCC", INPUT_FORMAT, outputFormat, "100", "100");
    byte[] image = imageGenerator.generateImage(imageDTO);
    assert image.length > 0;
  }

  @Test
  public void whenImageSizeProvided_thenImageSizeIsCorrect() throws Exception {
    int imageWidthAndHeight = 100;
    ImageDTO imageDTO =
        new ImageDTO(
            "CCC",
            INPUT_FORMAT,
            "png",
            String.valueOf(imageWidthAndHeight),
            String.valueOf(imageWidthAndHeight));
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
    ImageDTO imageDTO =
        new ImageDTO("CCC", INPUT_FORMAT, "png", imageWidthAndHeight, imageWidthAndHeight);
    byte[] bytes = imageGenerator.generateImage(imageDTO);
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
    assertEquals(defaultWidthAndHeight, image.getWidth());
    assertEquals(defaultWidthAndHeight, image.getHeight());
  }

  @ParameterizedTest
  @ValueSource(strings = {"gif", "txt", "pdf"})
  public void whenInvalidImageFormat_thenThrowException(String outputFormat) {
    ImageDTO imageDTO = new ImageDTO("CCC", INPUT_FORMAT, outputFormat, "100", "100");
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Failed to generate image with all available libraries.", exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNullOrEmptyImageFormat_thenThrowException(String outputFormat) {
    ImageDTO imageDTO = new ImageDTO("CCC", INPUT_FORMAT, outputFormat, "100", "100");
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> imageGenerator.generateImage(imageDTO));
    assertEquals("Failed to generate image with all available libraries.", exception.getMessage());
  }
}
