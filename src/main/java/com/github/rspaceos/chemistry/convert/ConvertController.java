package com.github.rspaceos.chemistry.convert;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
  public @ResponseBody String convert(@Valid @RequestBody ConvertDTO convertDTO) {
    return convertService.convertFormat(convertDTO);
  }
}
