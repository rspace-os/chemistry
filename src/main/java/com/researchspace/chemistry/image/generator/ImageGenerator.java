package com.researchspace.chemistry.image.generator;

import com.researchspace.chemistry.image.ImageDTO;
import java.util.Optional;

public interface ImageGenerator {

  Optional<byte[]> generateImage(ImageDTO imageDTO);
}
