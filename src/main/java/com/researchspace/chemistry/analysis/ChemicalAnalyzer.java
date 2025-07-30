package com.researchspace.chemistry.analysis;

import com.researchspace.chemistry.extract.ExtractionResult;

/**
 * Interface for chemical analysis operations.
 * This interface provides a common contract for different types of chemical analysis,
 * such as extraction and stoichiometry.
 */
public interface ChemicalAnalyzer {

  /**
   * Analyzes the chemical input and returns the result.
   *
   * @param input The chemical input to analyze
   * @param analysisType The type of analysis to perform
   * @return The result of the analysis
   */
  ExtractionResult analyze(String input, AnalysisType analysisType);
}