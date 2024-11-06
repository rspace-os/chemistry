package com.github.rspaceos.chemistry.extract;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IndigoExtractor implements Extractor {

  private final Indigo indigo = new Indigo();

  @Override
  public ExtractionResult extract(String input) {
    IndigoObject inputChemical = indigo.loadMolecule(input);

    List<Molecule> molecules = new ArrayList<>();

    // does each component need checked for subcomponents?
    for(IndigoObject component: inputChemical.iterateComponents()){
      molecules.add(makeMolecule(component));
    }

    ExtractionResult result = new ExtractionResult();
    result.setMoleculeInfo(molecules);
    return result;

  }

  private Molecule makeMolecule(IndigoObject inputChemical){
    int atomCount = tryIndigoIntOperation(inputChemical::countAtoms);
    int bondCount = tryIndigoIntOperation(inputChemical::countBonds);
    String formula = tryIndigoStringOperation(inputChemical::grossFormula);
    double mass = tryIndigoDoubleOperation(inputChemical::mostAbundantMass);

    return new Molecule.Builder()
        .atomCount(atomCount)
        .bondCount(bondCount)
        .formula(formula)
        .exactMass(mass)
        .build();
  }

  private int tryIndigoIntOperation(Supplier<Integer> indigoOperation){
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return 0;
    }
  }

  private double tryIndigoDoubleOperation(Supplier<Double> indigoOperation){
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return 0;
    }
  }

  private String tryIndigoStringOperation(Supplier<String> indigoOperation){
    try {
      return indigoOperation.get();
    } catch (IndigoException e) {
      return "";
    }
  }
}
