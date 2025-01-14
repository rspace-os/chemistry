package com.researchspace.chemistry.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.researchspace.chemistry.convert.ChemistryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IndigoExtractorTest {

  @Autowired IndigoExtractor indigoExtractor;

  @Test
  public void whenValidChemical_thenPropertiesExtracted() {
    String chemical = "CC";
    ExtractionResult result = indigoExtractor.extract(chemical);
    assertEquals(2, result.getMolecules().get(0).getAtomCount());
    assertEquals(1, result.getMolecules().get(0).getBondCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "1234"})
  public void whenInvalidChem_thenThrowsException(String chemical) {
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> indigoExtractor.extract(chemical));
    assertEquals(
        "Can't load input as molecule or reaction. Input: " + chemical, exception.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void whenNullOrEmptyChem_thenThrowsException(String chemical) {
    ChemistryException exception =
        assertThrows(ChemistryException.class, () -> indigoExtractor.extract(chemical));
    assertEquals("Input is empty", exception.getMessage());
  }
}
