package com.researchspace.chemistry.analysis;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.extract.ExtractionResult;
import com.researchspace.chemistry.util.IndigoFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IndigoChemicalAnalyser implements ChemicalAnalyser {

  private final IndigoFacade indigoFacade;

  public IndigoChemicalAnalyser(IndigoFacade indigoFacade) {
    this.indigoFacade = indigoFacade;
  }

  @Override
  public ExtractionResult analyse(String input, boolean groupMoleculesByRole) {
    IndigoObject inputChemical = indigoFacade.load(new Indigo(), input);

    return groupMoleculesByRole
        ? groupMoleculesByRole(inputChemical)
        : analyseMolecules(inputChemical);
  }

  /***
   * Analyses the input chemical to extract its formula, mass, and other properties.
   * Each molecule will have the generic role of MOLECULE.
   * If the input is a reaction, it will return the formula only.
   * @param inputChemical the IndigoObject representing the chemical input
   * @return ExtractionResult containing the chemical information
   */
  private ExtractionResult analyseMolecules(IndigoObject inputChemical) {
    boolean isReaction = tryStringOperation(inputChemical::dbgInternalType).contains("reaction");
    String formula = tryStringOperation(inputChemical::grossFormula);
    ExtractionResult result = new ExtractionResult();
    result.setFormula(formula);
    result.setReaction(isReaction);

    if (isReaction) {
      return result;
    }

    List<Molecule> molecules = new ArrayList<>();
    molecules.add(extractMoleculeInfo(inputChemical, MoleculeRole.MOLECULE));
    result.setMoleculeInfo(molecules);
    return result;
  }

  /***
   * Groups molecules in a reaction by their roles (reactant, product, agent).
   * Extracts the formula, mass, and other properties for each molecule.
   * @param inputChemical the IndigoObject representing the chemical input
   * @return ExtractionResult containing the grouped molecules and their properties
   */
  private ExtractionResult groupMoleculesByRole(IndigoObject inputChemical) {
    boolean isReaction = tryStringOperation(inputChemical::dbgInternalType).contains("reaction");
    String formula = tryStringOperation(inputChemical::grossFormula);
    ExtractionResult result = new ExtractionResult();
    result.setFormula(formula);
    result.setReaction(isReaction);
    List<Molecule> molecules = new ArrayList<>();

    if (!isReaction) {
      throw new ChemistryException("For Stoichiometry analysis, the input must be a reaction.");
    }

    inputChemical
        .iterateReactants()
        .forEach(
            molecule -> {
              Molecule reactant = extractMoleculeInfo(molecule, MoleculeRole.REACTANT);
              molecules.add(reactant);
            });

    inputChemical
        .iterateProducts()
        .forEach(
            molecule -> {
              Molecule product = extractMoleculeInfo(molecule, MoleculeRole.PRODUCT);
              molecules.add(product);
            });

    inputChemical
        .iterateCatalysts()
        .forEach(
            molecule -> {
              Molecule agent = extractMoleculeInfo(molecule, MoleculeRole.AGENT);
              molecules.add(agent);
            });

    result.setMoleculeInfo(molecules);
    return result;
  }

  private Molecule extractMoleculeInfo(IndigoObject molecule, MoleculeRole role) {
    return new Molecule.Builder()
        .atomCount(tryIntOperation(molecule::countAtoms))
        .bondCount(tryIntOperation(molecule::countBonds))
        .exactMass(tryDoubleOperation(molecule::mostAbundantMass))
        .formula(tryStringOperation(molecule::grossFormula))
        .mass(tryDoubleOperation(molecule::molecularWeight))
        .role(role)
        .smiles(tryStringOperation(molecule::smiles))
        .build();
  }

  private String tryStringOperation(Supplier<String> indigoOperation) {
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return "";
    }
  }

  private int tryIntOperation(Supplier<Integer> indigoOperation) {
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return 0;
    }
  }

  private double tryDoubleOperation(Supplier<Double> indigoOperation) {
    try {
      double result = indigoOperation.get();
      String rounded = String.format("%.2f", result);
      return Double.parseDouble(rounded);
    } catch (IndigoException e) {
      return 0;
    }
  }
}
