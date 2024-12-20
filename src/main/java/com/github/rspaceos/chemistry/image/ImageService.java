package com.github.rspaceos.chemistry.image;

import org.springframework.stereotype.Service;

@Service
public class ImageService {

  private final ImageGenerator imageGenerator;

  public ImageService(ImageGenerator imageGenerator) {
    this.imageGenerator = imageGenerator;
  }

  public byte[] exportImage(ImageDTO imageDTO) {
    return imageGenerator.generateImage(imageDTO);
  }
}
