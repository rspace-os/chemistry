package com.researchspace.chemistry.util;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.ConvertDTO;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndigoFacade {
  private static final Logger logger = LoggerFactory.getLogger(IndigoFacade.class);

  public Optional<String> convert(ConvertDTO convertDTO) {
    logger.info("Attempting conversion using Indigo.");
    Indigo indigo = new Indigo();
    indigo.setOption("ignore-stereochemistry-errors", true);

    IndigoObject indigoObject;
    try {
      indigoObject = load(indigo, convertDTO.input());
      String converted =
          switch (convertDTO.outputFormat()) {
            case "cdx" -> indigoObject.b64cdx();
            case "cdxml" -> indigoObject.cdxml();
            case "smiles", "smi" -> indigoObject.smiles();
            case "ket" -> indigoObject.json();
            case "mol" -> indigoObject.molfile();
            default -> "";
          };
      if (converted.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(converted);
    } catch (ChemistryException | IndigoException e) {
      logger.warn("Unable to convert with Indigo. {}", e.getMessage());
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
    try {
      indigoObject = loadNormalStructure(indigo, input);
    } catch (IndigoException e) {
      try {
        indigoObject = loadQueryStructure(indigo, input);
      } catch (IndigoException ex) {
        throw new ChemistryException(
            "Can't load input as molecule or reaction. Input: " + StringUtils.abbreviate(input, 50),
            ex);
      }
    }
    return indigoObject;
  }

  private IndigoObject loadQueryStructure(Indigo indigo, String input) {
    try {
      return indigo.loadQueryMolecule(input);
    } catch (IndigoException e) {
      return indigo.loadQueryReaction(input);
    }
  }

  private IndigoObject loadNormalStructure(Indigo indigo, String input) {
    try {
      return indigo.loadMolecule(input);
    } catch (IndigoException e) {
      return indigo.loadReaction(input);
    }
  }
}
