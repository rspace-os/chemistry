package com.researchspace.chemistry.convert;

import jakarta.validation.constraints.NotBlank;

public record ConvertDTO(
    @NotBlank String input, String inputFormat, @NotBlank String outputFormat) {
  public ConvertDTO(String input, String outputFormat) {
    this(input, "", outputFormat);
  }
}
