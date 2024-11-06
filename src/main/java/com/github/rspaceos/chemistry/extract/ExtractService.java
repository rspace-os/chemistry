package com.github.rspaceos.chemistry.extract;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExtractService {

  private final Extractor extractor;

  @Autowired
  public ExtractService(Extractor extractor) {
    this.extractor = extractor;
  }

  public ExtractionResult extract(ExtractionRequest requestDTO) {
    return extractor.extract(requestDTO.input());
  }
}
