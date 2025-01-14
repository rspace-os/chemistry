package com.researchspace.chemistry.image;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ImageController {
  private final ImageService imageService;

  @Autowired
  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @PostMapping(value = "/chemistry/image")
  public @ResponseBody byte[] exportImage(@Valid @RequestBody ImageDTO imageDTO) {
    return imageService.exportImage(imageDTO);
  }
}
