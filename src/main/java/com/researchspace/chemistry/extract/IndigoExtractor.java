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
    List<Molecule> molecules = new ArrayList<>();

    for (IndigoObject component : inputChemical.iterateComponents()) {
      molecules.add(makeMolecule(component));
    }

    ExtractionResult result = new ExtractionResult();
    result.setMoleculeInfo(molecules);
    result.setMolecules(molecules);
    return result;
  }

  private Molecule makeMolecule(IndigoObject inputChemical) {
    int atomCount = tryIndigoIntOperation(inputChemical::countAtoms);
    int bondCount = tryIndigoIntOperation(inputChemical::countBonds);
    double mass = tryIndigoDoubleOperation(inputChemical::mostAbundantMass);

    return new Molecule.Builder()
        .atomCount(atomCount)
        .bondCount(bondCount)
        .exactMass(mass)
        .role(MoleculeRole.MOLECULE)
        .build();
  }

  private int tryIndigoIntOperation(Supplier<Integer> indigoOperation) {
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return 0;
    }
  }

  private double tryIndigoDoubleOperation(Supplier<Double> indigoOperation) {
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return 0;
    }
  }
}
