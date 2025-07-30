package com.researchspace.chemistry.stoichiometry;

import com.researchspace.chemistry.analysis.ChemicalAnalysisService;
import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for stoichiometry analysis operations.
 */
@RestController
public class StoichiometryController {

  private final ChemicalAnalysisService analysisService;

  @Autowired
  public StoichiometryController(ChemicalAnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  /**
   * Analyzes the stoichiometry of a chemical reaction.
   *
   * @param requestDTO The request containing the chemical input to analyze
   * @return The result of the stoichiometry analysis
   */
  @PostMapping("/chemistry/stoichiometry")
  public ExtractionResult analyzeStoichiometry(@Valid @RequestBody ExtractionRequest requestDTO) {
    return analysisService.analyzeStoichiometry(requestDTO);
  }
}
