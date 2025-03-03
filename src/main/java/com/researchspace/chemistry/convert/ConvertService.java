package com.researchspace.chemistry.convert;

import com.researchspace.chemistry.ChemistryException;
import com.researchspace.chemistry.convert.convertor.Convertor;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ConvertService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertService.class);

  private final Convertor convertor;

  public ConvertService(@Qualifier("compositeConvertor") Convertor convertor) {
    this.convertor = convertor;
  }

  public String convert(ConvertDTO convertDTO) {
    String inputPreview = convertDTO.input().length() > 50 ? convertDTO.input().substring(0, 50) : convertDTO.input();
    LOGGER.info(
        "Converting format: {} with input: {} to output format: {}",
        convertDTO.inputFormat(),
        inputPreview,
        convertDTO.outputFormat());
    Optional<String> converted = convertor.convert(convertDTO);
    return converted.orElseThrow(
        () ->
            new ChemistryException(
                String.format("Unable to perform conversion to %s.", convertDTO.outputFormat())));
  }
}
