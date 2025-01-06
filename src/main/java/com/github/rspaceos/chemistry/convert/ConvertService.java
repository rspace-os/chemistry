package com.github.rspaceos.chemistry.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConvertService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertService.class);

  private final Convertor convertor;

  public ConvertService(Convertor convertor) {
    this.convertor = convertor;
  }

  public String convert(ConvertDTO convertDTO) {
    LOGGER.info(
        "Converting input: {} to output format: {}", convertDTO.input(), convertDTO.outputFormat());
    return convertor.convert(convertDTO);
  }
}
