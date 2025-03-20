package com.researchspace.chemistry.search;

import jakarta.validation.constraints.NotNull;

public record SaveDTO(@NotNull String chemical, @NotNull String chemicalId, String chemicalFormat) {
  public SaveDTO(String chemical, String chemicalId) {
    this(chemical, chemicalId, "");
  }
}
