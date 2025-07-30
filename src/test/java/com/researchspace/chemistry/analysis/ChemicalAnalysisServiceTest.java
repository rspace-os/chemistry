package com.researchspace.chemistry.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.researchspace.chemistry.extract.ExtractionRequest;
import com.researchspace.chemistry.extract.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChemicalAnalysisServiceTest {

  @Mock private ChemicalAnalyzer chemicalAnalyzer;

  private ChemicalAnalysisService analysisService;

  @BeforeEach
  void setUp() {
    analysisService = new ChemicalAnalysisService(chemicalAnalyzer);
  }

  @Test
  void extract_shouldCallAnalyzerWithExtractionType() {
    // Arrange
    ExtractionRequest request = new ExtractionRequest("CCC");
    ExtractionResult expectedResult = new ExtractionResult();
    when(chemicalAnalyzer.analyze(any(), any())).thenReturn(expectedResult);

    // Act
    ExtractionResult result = analysisService.extract(request);

    // Assert
    assertEquals(expectedResult, result);
    verify(chemicalAnalyzer).analyze(eq("CCC"), eq(AnalysisType.EXTRACTION));
  }

  @Test
  void analyzeStoichiometry_shouldCallAnalyzerWithStoichiometryType() {
    // Arrange
    ExtractionRequest request = new ExtractionRequest("(C(=O)O).(OCC)>>(C(=O)OCC).(O)");
    ExtractionResult expectedResult = new ExtractionResult();
    when(chemicalAnalyzer.analyze(any(), any())).thenReturn(expectedResult);

    // Act
    ExtractionResult result = analysisService.analyzeStoichiometry(request);

    // Assert
    assertEquals(expectedResult, result);
    verify(chemicalAnalyzer).analyze(eq("(C(=O)O).(OCC)>>(C(=O)OCC).(O)"), eq(AnalysisType.STOICHIOMETRY));
  }
}