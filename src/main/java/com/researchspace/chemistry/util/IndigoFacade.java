package com.researchspace.chemistry.util;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IndigoFacade {

  public Optional<String> convert(ConvertDTO convertDTO) {
    Indigo indigo = new Indigo();
    indigo.setOption("ignore-stereochemistry-errors", true);

    IndigoObject indigoObject;
    try {
      indigoObject = load(indigo, convertDTO.input());
      String converted =
          switch (convertDTO.outputFormat()) {
            case "cdxml" -> indigoObject.cdxml();
            case "smiles" -> indigoObject.smiles();
            case "ket" -> indigoObject.json();
            default -> "";
          };
      if (converted.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(converted);
    } catch (ChemistryException e) {
      return Optional.empty();
    }
  }

  /* input can be loaded as molecule or reaction but there doesn't seem to be a way to check
  which type it is either before attempting to load*/
  public IndigoObject load(Indigo indigo, String input) {
    if (input == null || input.isEmpty()) {
      throw new ChemistryException("Input is empty");
    }
    IndigoObject indigoObject;
    String check = indigo.checkStructure(input);
    if (check.contains("Error at loading structure")) {
      // probably a query structure
      try {
        indigoObject = indigo.loadQueryMolecule(input);
      } catch (IndigoException e) {
        try {
          indigoObject = indigo.loadQueryReaction(input);
        } catch (IndigoException ex) {
          throw new ChemistryException(
              "Can't load input as molecule or reaction. Input: " + input, ex);
        }
      }
    } else {
      // probably a normal structure - either molecule or reaction
      try {
        indigoObject = indigo.loadReaction(input);
      } catch (IndigoException e) {
        try {
          indigoObject = indigo.loadMolecule(input);
        } catch (IndigoException ex) {
          throw new ChemistryException(
              "Can't load input as molecule or reaction. Input: " + input, ex);
        }
      }
    }
    return indigoObject;
  }
}
