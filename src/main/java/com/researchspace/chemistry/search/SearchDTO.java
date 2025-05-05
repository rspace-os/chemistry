package com.researchspace.chemistry.search;

import jakarta.validation.constraints.NotBlank;

public record SearchDTO(
    @NotBlank String chemicalSearchTerm, String searchTermFormat, SearchType searchType) {

  public SearchDTO(String chemicalSearchTerm) {
    this(chemicalSearchTerm, "", SearchType.SUBSTRUCTURE);
  }

  public SearchDTO(String chemicalSearchTerm, String searchTermFormat) {
    this(chemicalSearchTerm, searchTermFormat, SearchType.SUBSTRUCTURE);
  }

  public SearchDTO(String chemicalSearchTerm, SearchType searchType) {
    this(chemicalSearchTerm, "", searchType);
  }
}
