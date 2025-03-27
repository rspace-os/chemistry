package com.researchspace.chemistry.image;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.image.generator.IndigoImageGenerator;
import com.researchspace.chemistry.image.generator.OpenBabelImageGenerator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

  private final IndigoImageGenerator indigoImageGenerator;

  private final OpenBabelImageGenerator openBabelImageGenerator;

  @Autowired
  public ImageService(
      IndigoImageGenerator indigoImageGenerator, OpenBabelImageGenerator openBabelImageGenerator) {
    this.indigoImageGenerator = indigoImageGenerator;
    this.openBabelImageGenerator = openBabelImageGenerator;
  }

  public byte[] exportImage(ImageDTO imageDTO) {
    LOGGER.info("Exporting image to: {}", imageDTO.outputFormat());
    Optional<byte[]> image = indigoImageGenerator.generateImage(imageDTO);

    if (image.isEmpty()) {
      image = openBabelImageGenerator.generateImage(imageDTO);
    }

    if (image.isPresent() && image.get().length > 0) {
      return image.get();
    } else {
      throw new ChemistryException("Failed to generate image with all available libraries.");
    }
  }
}
