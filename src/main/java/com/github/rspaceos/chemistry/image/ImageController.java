package com.github.rspaceos.chemistry.image;

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

  @PostMapping(value = "/chemistry/export")
  public @ResponseBody byte[] exportImage(@RequestBody ImageDTO imageDTO) {
    return imageService.exportImage(imageDTO);
  }
}
