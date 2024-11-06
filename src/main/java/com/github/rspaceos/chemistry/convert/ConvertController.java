package com.github.rspaceos.chemistry.convert;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConvertController {

  private final ConvertService convertService;

  public ConvertController(ConvertService convertService) {
    this.convertService = convertService;
  }

  @PostMapping("/chemistry/convert")
  public String convert(@RequestBody ConvertDTO convertDTO){
    return convertService.convert(convertDTO);
  }

}
