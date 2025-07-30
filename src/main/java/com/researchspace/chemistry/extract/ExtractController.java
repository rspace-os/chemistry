package com.researchspace.chemistry.extract;

import com.researchspace.chemistry.analysis.ChemicalAnalysisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExtractController {

  private final ChemicalAnalysisService analysisService;

  @Autowired
  public ExtractController(ChemicalAnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @PostMapping("/chemistry/extract")
  public ExtractionResult extract(@Valid @RequestBody ExtractionRequest requestDTO) {
    return analysisService.extract(requestDTO);
  }
}
