package com.researchspace.chemistry.extract;

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
    LOGGER.info("Extracting metadata from input: {}...(truncated)", requestDTO.input().substring(0, Math.min(requestDTO.input().length(), 50)));
    return extractor.extract(requestDTO.input());
  }
}
