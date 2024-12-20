package com.github.rspaceos.chemistry.image;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.epam.indigo.IndigoRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndigoImageGenerator implements ImageGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndigoImageGenerator.class);

  @Override
  public byte[] generateImage(ImageDTO imageDTO) {
    LOGGER.info("Exporting image to: {}", imageDTO.outputFormat());
    if (imageDTO.outputFormat().equals("jpg") || imageDTO.outputFormat().equals("jpeg")) {
      return convertPngToJpg(imageDTO.input());
    } else {
      return render(imageDTO.input(), imageDTO.outputFormat());
    }
  }

  private byte[] convertPngToJpg(String input) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    try {
      File tmpPng = File.createTempFile("pre", ".png");
      IndigoObject indigoObject = load(indigo, input);
      indigo.setOption("render-output-format", "png");
      indigo.setOption("render-margins", 10, 10);
      indigoObject.layout();
      renderer.renderToFile(indigoObject, tmpPng.getPath());

      BufferedImage bufferedImage = ImageIO.read(tmpPng);
      ByteArrayOutputStream bufferedImageOut = new ByteArrayOutputStream();

      // create a blank, RGB, same width and height
      BufferedImage newBufferedImage =
          new BufferedImage(
              bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

      // draw a white background and puts the originalImage on it.
      newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);

      // save an image
      ImageIO.write(newBufferedImage, "jpg", bufferedImageOut);

      return bufferedImageOut.toByteArray();
      // todo: clear tmp file
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] render(String input, String outputFormat) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    indigo.setOption("ignore-stereochemistry-errors", true);
    IndigoObject indigoObject = load(indigo, input);

    indigo.setOption("render-output-format", outputFormat);
    indigo.setOption("render-margins", 10, 10);
    indigo.setOption("render-image-size", "1000,1000");
    indigoObject.layout();
    byte[] image = renderer.renderToBuffer(indigoObject);
    renderer.renderToFile(indigoObject, "file-system-file." + outputFormat);
    return image;
  }

  private IndigoObject load(Indigo indigo, String input) {
    // input can be loaded as molecule or reaction but there doesn't seem to be a way to check
    // which type it is either before or after attempting to load
    IndigoObject indigoObject;
    try {
      indigoObject = indigo.loadMolecule(input);
    } catch (IndigoException e) {
      indigoObject = indigo.loadReaction(input);
    }
    return indigoObject;
  }
}
