package com.researchspace.chemistry.image;

import com.researchspace.chemistry.image.generator.ImageGeneratorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

  private final ImageGeneratorChain imageGenerator;

  public ImageService(ImageGeneratorChain imageGenerator) {
    this.imageGenerator = imageGenerator;
  }

  public byte[] exportImage(ImageDTO imageDTO) {
    LOGGER.info("Exporting image to: {}", imageDTO.outputFormat());
    return imageGenerator.generateImage(imageDTO);
  }
}
