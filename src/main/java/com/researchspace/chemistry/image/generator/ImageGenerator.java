package com.researchspace.chemistry.image.generator;

import com.researchspace.chemistry.image.ImageDTO;

public interface ImageGenerator {

  byte[] generateImage(ImageDTO imageDTO);
}
