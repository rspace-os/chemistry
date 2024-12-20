package com.github.rspaceos.chemistry.convert;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndigoConvertor implements Convertor {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndigoConvertor.class);

  @Override
  public String convert(ConvertDTO convertDTO) {
    Indigo indigo = new Indigo();
    indigo.setOption("ignore-stereochemistry-errors", true);

    // input can be loaded as molecule or reaction but there doesn't seem to be a way to check
    // which type it is either before or after attempting to load
    IndigoObject indigoObject = load(indigo, convertDTO.input());

    LOGGER.info("Converting to format: {}", convertDTO.outputFormat());

    return switch (convertDTO.outputFormat()) {
      case "cdxml" -> indigoObject.cdxml();
      case "smiles" -> indigoObject.smiles();
      case "rxn", "rxnfile" -> indigoObject.rxnfile();
      case "ket" -> indigoObject.json();
      default -> {
        LOGGER.warn("Cannot convert to {}", convertDTO.outputFormat());
        yield "";
      }
    };
  }

  private IndigoObject load(Indigo indigo, String input) {
    // input can be loaded as molecule or reaction but there doesn't seem to be a way to check
    // which type it is either before or after attempting to load
    IndigoObject indigoObject;
    try {
      indigoObject = indigo.loadMolecule(input);
    } catch (IndigoException e) {
      indigoObject = indigo.loadReaction(input);
    }
    return indigoObject;
  }
}
