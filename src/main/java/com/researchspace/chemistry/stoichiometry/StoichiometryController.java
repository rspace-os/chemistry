package com.researchspace.chemistry.stoichiometry;

import com.researchspace.chemistry.analysis.ChemicalAnalyser;
import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoichiometryController {
  private static final Logger LOGGER = LoggerFactory.getLogger(StoichiometryController.class);

  private final ChemicalAnalyser chemicalAnalyser;

  @Autowired
  public StoichiometryController(ChemicalAnalyser chemicalAnalyser) {
    this.chemicalAnalyser = chemicalAnalyser;
  }

  @PostMapping("/chemistry/stoichiometry")
  public @ResponseBody ExtractionResult analyse(@Valid @RequestBody ExtractionRequest requestDTO) {
    String inputPreview = StringUtils.abbreviate(requestDTO.input(), 50);
    LOGGER.info("Analysing stoichiometry from input: {}", inputPreview);
    boolean groupMoleculesByRole = true;
    return chemicalAnalyser.analyse(requestDTO.input(), groupMoleculesByRole);
  }
}
