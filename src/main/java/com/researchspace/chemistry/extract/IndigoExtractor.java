package com.researchspace.chemistry.extract;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.researchspace.chemistry.util.IndigoFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IndigoExtractor implements Extractor {

  private final IndigoFacade indigoFacade;

  public IndigoExtractor(IndigoFacade indigoFacade) {
    this.indigoFacade = indigoFacade;
  }

  @Override
  public ExtractionResult extract(String input) {
    IndigoObject inputChemical = indigoFacade.load(new Indigo(), input);
    return getExtractionResult(inputChemical);
  }

  private ExtractionResult getExtractionResult(IndigoObject inputChemical) {
    boolean isReaction = tryStringOperation(inputChemical::dbgInternalType).contains("reaction");
    String formula = tryStringOperation(inputChemical::grossFormula);
    ExtractionResult result = new ExtractionResult();
    result.setFormula(formula);
    result.setReaction(isReaction);
    // for reactions, only the formula is displayed
    if(isReaction) {
      return result;
    }

    List<Molecule> molecules = new ArrayList<>();
    int atomCount = tryIntOperation(inputChemical::countAtoms);
    int bondCount = tryIntOperation(inputChemical::countBonds);
    double mass = tryDoubleOperation(inputChemical::mostAbundantMass);
    double molWeight = tryDoubleOperation(inputChemical::molecularWeight);


    molecules.add(new Molecule.Builder()
            .atomCount(atomCount)
            .bondCount(bondCount)
            .exactMass(mass)
            .formula(formula)
            .mass(molWeight)
            .role(MoleculeRole.MOLECULE)
            .build());

    result.setMoleculeInfo(molecules);
    result.setMolecules(molecules);
    return result;
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
