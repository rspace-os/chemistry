package com.researchspace.chemistry.convert.convertor;

import com.researchspace.chemistry.convert.ConvertDTO;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CompositeConvertor implements Convertor {
  private final Convertor openBabelConvertor;

  private final Convertor indigoConvertor;

  @Autowired
  public CompositeConvertor(
      @Qualifier("openBabelConvertor") Convertor openBabel,
      @Qualifier("indigoConvertor") Convertor indigo) {
    this.openBabelConvertor = openBabel;
    this.indigoConvertor = indigo;
  }

  @Override
  public Optional<String> convert(ConvertDTO convertDTO) {
    Optional<String> converted = indigoConvertor.convert(convertDTO);
    if (converted.isEmpty() && canBeConvertedByOpenBabel(convertDTO.inputFormat())) {
      converted = openBabelConvertor.convert(convertDTO);
    }
    return converted;
  }

  // OpenBabel needs to know the input format before attempting conversion
  private boolean canBeConvertedByOpenBabel(String inputFormat) {
    return inputFormat != null && !inputFormat.isEmpty();
  }
}
