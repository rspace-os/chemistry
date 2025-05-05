package com.researchspace.chemistry.search;

import jakarta.validation.constraints.NotBlank;

public record SaveDTO(
    @NotBlank String chemical, @NotBlank String chemicalId, String chemicalFormat) {
  public SaveDTO(String chemical, String chemicalId) {
    this(chemical, chemicalId, "");
  }
}
