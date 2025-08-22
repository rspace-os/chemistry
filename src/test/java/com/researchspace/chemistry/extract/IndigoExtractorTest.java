package com.researchspace.chemistry.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.analysis.IndigoChemicalAnalyser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IndigoExtractorTest {

  @Autowired IndigoChemicalAnalyser chemicalAnalyser;

  private static final boolean GROUP_BY_ROLE = false;

  @Test
  public void whenValidChemical_thenPropertiesExtracted() {
    String chemical = "CC";
    ExtractionResult result = chemicalAnalyser.analyse(chemical, GROUP_BY_ROLE);
    assertEquals(2, result.getMoleculeInfo().get(0).getAtomCount());
    assertEquals(1, result.getMoleculeInfo().get(0).getBondCount());
    assertEquals("C2 H6", result.getMoleculeInfo().get(0).getFormula());
    assertEquals(30.05, result.getMoleculeInfo().get(0).getExactMass());
    assertEquals(30.07, result.getMoleculeInfo().get(0).getMass());
    assertFalse(result.isReaction());
  }

  @Test
  public void whenChemicalElementIsAReaction_thenOnlyFormulaIsExtracted() {
    String reaction = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";
    ExtractionResult result = chemicalAnalyser.analyse(reaction, GROUP_BY_ROLE);
    assertTrue(result.isReaction());
    assertTrue(result.getMoleculeInfo().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "1234"})
  public void whenInvalidChem_thenThrowsException(String chemical) {
    ChemistryException exception =
        assertThrows(
            ChemistryException.class, () -> chemicalAnalyser.analyse(chemical, GROUP_BY_ROLE));
    assertEquals(
        "Can't load input as molecule or reaction. Input: " + chemical, exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNullOrEmptyChem_thenThrowsException(String chemical) {
    ChemistryException exception =
        assertThrows(
            ChemistryException.class, () -> chemicalAnalyser.analyse(chemical, GROUP_BY_ROLE));
    assertEquals("Input is empty", exception.getMessage());
  }
}
