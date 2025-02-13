package com.researchspace.chemistry.convert.convertor;

import com.researchspace.chemistry.convert.ConvertDTO;
import com.researchspace.chemistry.util.IndigoFacade;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IndigoConvertor implements Convertor {
  private final IndigoFacade indigo;

  public IndigoConvertor(IndigoFacade indigo) {
    this.indigo = indigo;
  }

  @Override
  public Optional<String> convert(ConvertDTO convertDTO) {
    return indigo.convert(convertDTO);
  }
}
