package com.researchspace.chemistry.extract;

import com.researchspace.chemistry.analysis.ChemicalAnalyser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExtractController {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtractController.class);

  private final ChemicalAnalyser chemicalAnalyser;

  @Autowired
  public ExtractController(ChemicalAnalyser chemicalAnalyser) {
    this.chemicalAnalyser = chemicalAnalyser;
  }

  @PostMapping("/chemistry/extract")
  public ExtractionResult extract(@Valid @RequestBody ExtractionRequest requestDTO) {
    String inputPreview = StringUtils.abbreviate(requestDTO.input(), 50);
    LOGGER.info("Extracting from input: {}", inputPreview);
    boolean groupMoleculesByRole = false;
    return chemicalAnalyser.analyse(requestDTO.input(), groupMoleculesByRole);
  }
}
