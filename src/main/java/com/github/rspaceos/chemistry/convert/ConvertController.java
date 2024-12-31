package com.github.rspaceos.chemistry.convert;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConvertController {

  private final Convertor convertor;

  public ConvertController(Convertor convertor) {
    this.convertor = convertor;
  }

  @PostMapping(value = "/chemistry/convert")
  public @ResponseBody String convert(@Valid @RequestBody ConvertDTO convertDTO) {
    return convertor.convert(convertDTO);
  }
}
