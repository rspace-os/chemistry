package com.github.rspaceos.chemistry.convert;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConvertController {

  private final ConvertService convertService;

  public ConvertController(ConvertService convertService) {
    this.convertService = convertService;
  }

  @PostMapping(value = "/chemistry/convert")
  public @ResponseBody String convert(@RequestBody ConvertDTO convertDTO){
    return convertService.convertFormat(convertDTO);
  }

  @PostMapping(value = "/chemistry/export")
  public @ResponseBody byte[] exportImage(@RequestBody ConvertDTO convertDTO){
    return convertService.exportImage(convertDTO);
  }
}
