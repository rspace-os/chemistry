package com.researchspace.chemistry.analysis;

import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for performing chemical analysis operations.
 * This service provides methods for different types of chemical analysis,
 * such as extraction and stoichiometry.
 */
@Service
public class ChemicalAnalysisService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChemicalAnalysisService.class);

  private final ChemicalAnalyzer chemicalAnalyzer;

  @Autowired
  public ChemicalAnalysisService(ChemicalAnalyzer chemicalAnalyzer) {
    this.chemicalAnalyzer = chemicalAnalyzer;
  }

  /**
   * Performs extraction analysis on the given input.
   *
   * @param request The extraction request containing the input to analyze
   * @return The result of the extraction analysis
   */
  public ExtractionResult extract(ExtractionRequest request) {
    String inputPreview = StringUtils.abbreviate(request.input(), 50);
    LOGGER.info("Extracting from input: {}", inputPreview);
    return chemicalAnalyzer.analyze(request.input(), AnalysisType.EXTRACTION);
  }

  /**
   * Performs stoichiometry analysis on the given input.
   *
   * @param request The extraction request containing the input to analyze
   * @return The result of the stoichiometry analysis
   */
  public ExtractionResult analyzeStoichiometry(ExtractionRequest request) {
    String inputPreview = StringUtils.abbreviate(request.input(), 50);
    LOGGER.info("Analyzing stoichiometry from input: {}", inputPreview);
    return chemicalAnalyzer.analyze(request.input(), AnalysisType.STOICHIOMETRY);
  }
}