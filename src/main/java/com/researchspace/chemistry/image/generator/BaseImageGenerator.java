package com.researchspace.chemistry.image.generator;

import com.researchspace.chemistry.image.ImageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Abstract base class for image generators, implementing the {@link ImageGenerator} interface.
 * Provides support for chaining multiple image generators and error handling during the image
 * generation process.
 * This class is designed to be extended by specific implementations of image generators.
 * The chain-of-responsibility pattern is utilized to delegate the generation process to the
 * next image generator in the chain if the current implementation cannot handle the request.
 * Subclasses are expected to provide a concrete implementation of the {@code generateImage} method
 * defined in the {@link ImageGenerator} interface.
 */
@Service
public abstract class BaseImageGenerator implements ImageGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseImageGenerator.class);
  private ImageGenerator next;

  public void setNext(ImageGenerator next) {
    this.next = next;
  }

  protected byte[] tryNext(ImageDTO imageDTO) {
    if (next != null) {
      return next.generateImage(imageDTO);
    }
    return new byte[0];
  }

  protected byte[] handleError(String libraryName, Exception e, ImageDTO imageDTO) {
    LOGGER.warn("Failed to generate image with {}: {}", libraryName, e.getMessage());
    return tryNext(imageDTO);
  }
}
