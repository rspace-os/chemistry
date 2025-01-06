package com.github.rspaceos.chemistry.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExtractService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtractService.class);

  private final Extractor extractor;

  @Autowired
  public ExtractService(Extractor extractor) {
    this.extractor = extractor;
  }

  public ExtractionResult extract(ExtractionRequest requestDTO) {
    LOGGER.info("Extracting from input: {}", requestDTO.input());
    return extractor.extract(requestDTO.input());
  }
}
