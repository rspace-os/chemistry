package com.researchspace.chemistry.convert;

import jakarta.validation.constraints.NotNull;

public record ConvertDTO(@NotNull String input, String inputFormat, @NotNull String outputFormat) {
  public ConvertDTO(String input, String outputFormat) {
    this(input, "", outputFormat);
  }
}
