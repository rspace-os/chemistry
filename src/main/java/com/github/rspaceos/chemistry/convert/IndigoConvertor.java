package com.github.rspaceos.chemistry.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndigoConvertor implements Convertor {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndigoConvertor.class);

  private final IndigoFacade indigo;

  public IndigoConvertor(IndigoFacade indigo) {
    this.indigo = indigo;
  }

  @Override
  public String convert(ConvertDTO convertDTO) throws ChemistryException {
    return indigo.convert(convertDTO);
  }
}
