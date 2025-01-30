package com.researchspace.chemistry.image;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.epam.indigo.IndigoRenderer;
import com.researchspace.chemistry.convert.ChemistryException;
import com.researchspace.chemistry.util.IndigoFacade;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class IndigoImageGenerator implements ImageGenerator {
  private static final String DEFAULT_WIDTH_HEIGHT = "500";

  private final IndigoFacade indigoFacade;

  public IndigoImageGenerator(IndigoFacade indigoFacade) {
    this.indigoFacade = indigoFacade;
  }

  @Override
  public byte[] generateImage(ImageDTO imageDTO) {
    String outputFormat = imageDTO.outputFormat();
    if (outputFormat == null || outputFormat.isEmpty()) {
      throw new ChemistryException("Output format is empty");
    }
    return switch (outputFormat) {
      case "jpg", "jpeg" -> convertPngToJpg(imageDTO);
      case "png", "svg" -> render(imageDTO);
      default -> throw new ChemistryException("Unsupported image format: " + outputFormat);
    };
  }

  private byte[] convertPngToJpg(ImageDTO imageDTO) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    try {
      File tmpPng = File.createTempFile("pre", ".png");
      IndigoObject indigoObject = indigoFacade.load(indigo, imageDTO.input());
      indigo.setOption("render-output-format", "png");
      indigo.setOption("render-margins", 10, 10);
      indigoObject.layout();
      renderer.renderToFile(indigoObject, tmpPng.getPath());

      BufferedImage bufferedImage = ImageIO.read(tmpPng);
      ByteArrayOutputStream bufferedImageOut = new ByteArrayOutputStream();

      BufferedImage newBufferedImage =
          new BufferedImage(
              bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

      newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);
      ImageIO.write(newBufferedImage, "jpg", bufferedImageOut);
      tmpPng.delete();
      return bufferedImageOut.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] render(ImageDTO imageDTO) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    indigo.setOption("ignore-stereochemistry-errors", true);
    IndigoObject indigoObject = indigoFacade.load(indigo, imageDTO.input());

    indigo.setOption("render-output-format", imageDTO.outputFormat());
    indigo.setOption("render-margins", 10, 10);
    indigo.setOption("render-image-size", generateImageSize(imageDTO));
    indigoObject.layout();
    try {
      return renderer.renderToBuffer(indigoObject);
    } catch (IndigoException e) {
      throw new ChemistryException("Error rendering image", e);
    }
  }

  private String generateImageSize(ImageDTO imageDTO) {
    String imageSizeFormat = "%s,%s";
    if (StringUtils.isEmpty(imageDTO.width()) || StringUtils.isEmpty(imageDTO.height())) {
      return String.format(imageSizeFormat, DEFAULT_WIDTH_HEIGHT, DEFAULT_WIDTH_HEIGHT);
    }
    return String.format(imageSizeFormat, imageDTO.width(), imageDTO.height());
  }
}
