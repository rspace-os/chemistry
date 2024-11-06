package com.github.rspaceos.chemistry.convert;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoObject;
import com.epam.indigo.IndigoRenderer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConvertService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertService.class);

  public String convert(ConvertDTO convertDTO) {
    if(isImageFormat(convertDTO.outputFormat())){
      return generateImage(convertDTO);
    } else {
      return convertFormat(convertDTO);
    }
  }

  private String convertFormat(ConvertDTO convertDTO) {
    Indigo indigo = new Indigo();
    IndigoObject mol = indigo.loadMolecule(convertDTO.input());

    LOGGER.info("Converting: {} to format: {}.}", convertDTO.input(), convertDTO.outputFormat());

    return switch (convertDTO.outputFormat()) {
      case "cdxml" -> mol.cdxml();
      case "smiles" -> mol.smiles();
      case "rxn", "rxnfile" -> mol.rxnfile();
      case "ket" -> mol.json();
      default -> {
        LOGGER.warn("Cannot convert to {}", convertDTO.outputFormat());
        yield "";
      }
    };
  }

  private String generateImage(ConvertDTO convertDTO) {
    LOGGER.info("Converting: {} to format: {}", convertDTO.input(), convertDTO.outputFormat());
    if(convertDTO.outputFormat().equals("jpg") || convertDTO.outputFormat().equals("jpeg")){
      return convertPngToJpg(convertDTO.input());
    } else {
      return render(convertDTO.input(), convertDTO.outputFormat());
    }
  }

  private String convertPngToJpg(String input) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    try {
      File tmpPng = File.createTempFile("pre", ".png");
      IndigoObject mol = indigo.loadMolecule(input);
      indigo.setOption("render-output-format", "png");
      indigo.setOption("render-margins", 10, 10);
      mol.layout();
      renderer.renderToFile(mol, tmpPng.getPath());

      BufferedImage bufferedImage = ImageIO.read(tmpPng);
      ByteArrayOutputStream bufferedImageOut = new ByteArrayOutputStream();

      // create a blank, RGB, same width and height
      BufferedImage newBufferedImage = new BufferedImage(
          bufferedImage.getWidth(),
          bufferedImage.getHeight(),
          BufferedImage.TYPE_INT_RGB);

      // draw a white background and puts the originalImage on it.
      newBufferedImage.createGraphics()
          .drawImage(bufferedImage,
              0,
              0,
              Color.WHITE,
              null);

      // save an image
      ImageIO.write(newBufferedImage, "jpg", bufferedImageOut);

      return bufferedImageOut.toString();
      //todo: clear tmp file
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String render(String input, String outputFormat) {
    Indigo indigo = new Indigo();
    IndigoRenderer renderer = new IndigoRenderer(indigo);
    IndigoObject mol = indigo.loadMolecule(input);
    indigo.setOption("render-output-format", outputFormat);
    indigo.setOption("render-margins", 10, 10);
    mol.layout();
    byte[] image = renderer.renderToBuffer(mol);
    return new String(image);
  }

  private boolean isImageFormat(String format) {
    return format.equals("png") || format.equals("jpg") || format.equals("jpeg") || format.equals("svg");
  }

}
