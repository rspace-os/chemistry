package com.researchspace.chemistry.image.generator;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.image.ImageDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * Responsible for generating chemical structure images by orchestrating a chain of image
 * generators. It delegates image generation requests to a chain of generators until a valid image
 * is successfully created or all generators fail. If all generators in the chain fail to produce a
 * valid image, a {@code ChemistryException} is thrown. Images generation is attempted as follows:
 * 1. Using indigo in the original format: images retain all/most information from the original 2.
 * Using indigo converted first to MOL: higher success rate than 1 but with images which are less
 * representative of the original 3. Using OpenBabel: higher success rate than 1 and 2 but with
 * images which are less representative of the input.
 */
@Service
public class ImageGeneratorChain {
  private final IndigoOriginalFormatImageGenerator indigoOriginal;
  private final IndigoMolImageGenerator indigoMol;
  private final OpenBabelOriginalFormatImageGenerator openBabelOriginal;
  private ImageGenerator imageGeneratorChain;

  public ImageGeneratorChain(
      IndigoOriginalFormatImageGenerator indigoOriginal,
      IndigoMolImageGenerator indigoMol,
      OpenBabelOriginalFormatImageGenerator openBabelOriginal) {
    this.indigoOriginal = indigoOriginal;
    this.indigoMol = indigoMol;
    this.openBabelOriginal = openBabelOriginal;
  }

  @PostConstruct
  public void setup() {
    imageGeneratorChain = createGeneratorChain();
  }

  public byte[] generateImage(ImageDTO imageDTO) {
    byte[] image = imageGeneratorChain.generateImage(imageDTO);

    if (image != null && image.length > 0) {
      return image;
    } else {
      throw new ChemistryException("Failed to generate image with all available libraries.");
    }
  }

  private ImageGenerator createGeneratorChain() {
    indigoOriginal.setNext(indigoMol);
    indigoMol.setNext(openBabelOriginal);
    return indigoOriginal;
  }
}
