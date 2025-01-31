package com.researchspace.chemistry.convert;

import com.researchspace.chemistry.util.IndigoFacade;
import org.springframework.stereotype.Service;

@Service
public class IndigoConvertor implements Convertor {
  private final IndigoFacade indigo;

  public IndigoConvertor(IndigoFacade indigo) {
    this.indigo = indigo;
  }

  @Override
  public String convert(ConvertDTO convertDTO) throws ChemistryException {
    return indigo.convert(convertDTO);
  }
}
