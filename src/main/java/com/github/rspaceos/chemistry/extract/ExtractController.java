package com.github.rspaceos.chemistry.extract;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExtractController {

  private final ExtractService extractService;

  @Autowired
  public ExtractController(ExtractService extractService) {
    this.extractService = extractService;
  }

  @PostMapping("/chemistry/extract")
  public ExtractionResult extract(@Valid @RequestBody ExtractionRequest requestDTO) {
    return extractService.extract(requestDTO);
  }
}
