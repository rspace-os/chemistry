package com.researchspace.chemistry.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.extract.ExtractionResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class IndigoChemicalAnalyserTest {

  @Autowired private IndigoChemicalAnalyser chemicalAnalyser;

  private static final boolean GROUP_BY_ROLE = true;

  @Test
  public void whenAnalyzingStoichiometry_withReaction_thenExtractsReactantsAndProducts() {
    String reaction = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";

    ExtractionResult result = chemicalAnalyser.analyse(reaction, GROUP_BY_ROLE);

    assertTrue(result.isReaction());
    assertNotNull(result.getMoleculeInfo());
    assertFalse(result.getMoleculeInfo().isEmpty());

    List<Molecule> reactants = result.getReactants();
    assertFalse(reactants.isEmpty());
    assertEquals(MoleculeRole.REACTANT, reactants.get(0).getRole());

    List<Molecule> products = result.getProducts();
    assertFalse(products.isEmpty());
    assertEquals(MoleculeRole.PRODUCT, products.get(0).getRole());
  }

  @Test
  public void whenAnalyzingStoichiometry_withNonReaction_thenThrowException() {
    String nonReaction = "CCC";

    ChemistryException exception = assertThrows(ChemistryException.class, () -> {
      chemicalAnalyser.analyse(nonReaction, GROUP_BY_ROLE);
    });

    assertEquals("For Stoichiometry analysis, the input must be a reaction.", exception.getMessage());
  }

  @Test
  public void whenAnalyzingStoichiometry_thenMoleculePropertiesAreExtracted() {
    String reaction = "(C(=O)O).(OCC)>>(C(=O)OCC).(O)";

    ExtractionResult result = chemicalAnalyser.analyse(reaction, GROUP_BY_ROLE);

    List<Molecule> molecules = result.getMoleculeInfo();
    assertFalse(molecules.isEmpty());
    
    Molecule molecule = molecules.get(0);
    assertTrue(molecule.getAtomCount() >= 0);
    assertTrue(molecule.getBondCount() >= 0);
    assertNotNull(molecule.getFormula());
    assertTrue(molecule.getExactMass() >= 0);
    assertTrue(molecule.getMass() >= 0);
  }
}