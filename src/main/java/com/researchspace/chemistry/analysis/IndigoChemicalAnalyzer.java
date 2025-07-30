package com.researchspace.chemistry.analysis;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.researchspace.chemistry.extract.ExtractionResult;
import com.researchspace.chemistry.util.IndigoFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Implementation of the ChemicalAnalyzer interface using the Indigo library.
 * This class provides functionality for both extraction and stoichiometry analysis.
 */
@Service
public class IndigoChemicalAnalyzer implements ChemicalAnalyzer {

  private final IndigoFacade indigoFacade;

  public IndigoChemicalAnalyzer(IndigoFacade indigoFacade) {
    this.indigoFacade = indigoFacade;
  }

  @Override
  public ExtractionResult analyze(String input, AnalysisType analysisType) {
    IndigoObject inputChemical = indigoFacade.load(new Indigo(), input);

    return switch (analysisType) {
      case EXTRACTION -> extractInformation(inputChemical);
      case STOICHIOMETRY -> analyzeStoichiometry(inputChemical);
    };
  }

  /**
   * Extracts information about the chemical structure.
   * This method is used for the EXTRACTION analysis type.
   *
   * @param inputChemical The chemical structure to analyze
   * @return The extraction result
   */
  private ExtractionResult extractInformation(IndigoObject inputChemical) {
    boolean isReaction = tryStringOperation(inputChemical::dbgInternalType).contains("reaction");
    String formula = tryStringOperation(inputChemical::grossFormula);
    ExtractionResult result = new ExtractionResult();
    result.setFormula(formula);
    result.setReaction(isReaction);

    // for reactions, only the formula is displayed in extraction mode
    if (isReaction) {
      return result;
    }

    List<Molecule> molecules = new ArrayList<>();
    int atomCount = tryIntOperation(inputChemical::countAtoms);
    int bondCount = tryIntOperation(inputChemical::countBonds);
    double mass = tryDoubleOperation(inputChemical::mostAbundantMass);
    double molWeight = tryDoubleOperation(inputChemical::molecularWeight);

    molecules.add(
        new Molecule.Builder()
            .atomCount(atomCount)
            .bondCount(bondCount)
            .exactMass(mass)
            .formula(formula)
            .mass(molWeight)
            .role(MoleculeRole.MOLECULE)
            .build());

    result.setMoleculeInfo(molecules);
    return result;
  }

  /**
   * Analyzes the stoichiometry of a chemical reaction.
   * This method is used for the STOICHIOMETRY analysis type.
   *
   * @param inputChemical The chemical structure to analyze
   * @return The stoichiometry analysis result
   */
  private ExtractionResult analyzeStoichiometry(IndigoObject inputChemical) {
    boolean isReaction = tryStringOperation(inputChemical::dbgInternalType).contains("reaction");
    String formula = tryStringOperation(inputChemical::grossFormula);
    ExtractionResult result = new ExtractionResult();
    result.setFormula(formula);
    result.setReaction(isReaction);

    if (!isReaction) {
      // Stoichiometry analysis only makes sense for reactions
      return result;
    }

    List<Molecule> molecules = new ArrayList<>();
    
    // Extract reactants
    inputChemical.iterateReactants().forEach(molecule -> {
      Molecule reactant = extractMoleculeInfo(molecule, MoleculeRole.REACTANT);
      molecules.add(reactant);
    });
    
    // Extract products
    inputChemical.iterateProducts().forEach(molecule -> {
      Molecule product = extractMoleculeInfo(molecule, MoleculeRole.PRODUCT);
      molecules.add(product);
    });
    
    // Extract catalysts/agents
    inputChemical.iterateCatalysts().forEach(molecule -> {
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