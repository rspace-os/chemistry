package com.github.rspaceos.chemistry.extract;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.github.rspaceos.chemistry.convert.ConvertService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndigoExtractor implements Extractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndigoExtractor.class);

  @Override
  public ExtractionResult extract(String input) {
    Indigo indigo = new Indigo();

    IndigoObject inputChemical;
    try{
      inputChemical = indigo.loadMolecule(input);
      return getExtractionResult(inputChemical);
    } catch (IndigoException e) {
      try{
        inputChemical = indigo.loadReaction(input);
        return getExtractionResult(inputChemical);
      } catch (IndigoException e1) {
        LOGGER.warn("Unable to parse chemical input: {}", input, e1);
      }
    }
    return new ExtractionResult();
  }

  private ExtractionResult getExtractionResult(IndigoObject inputChemical) {
    List<Molecule> molecules = new ArrayList<>();

    // does each component need checked for subcomponents?
    for(IndigoObject component: inputChemical.iterateComponents()){
      molecules.add(makeMolecule(component));
    }

    ExtractionResult result = new ExtractionResult();
    result.setMoleculeInfo(molecules);
    result.setMolecules(molecules);
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
        .role(MoleculeRole.MOLECULE)
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
