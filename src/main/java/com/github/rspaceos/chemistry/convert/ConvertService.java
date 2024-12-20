package com.github.rspaceos.chemistry.convert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConvertService {
  private final Convertor convertor;

  @Autowired
  public ConvertService(Convertor convertor) {
    this.convertor = convertor;
  }

  public String convertFormat(ConvertDTO convertDTO) {
    return convertor.convert(convertDTO);
  }
}
