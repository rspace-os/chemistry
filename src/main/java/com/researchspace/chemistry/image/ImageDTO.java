package com.researchspace.chemistry.image;

import jakarta.validation.constraints.NotBlank;

public record ImageDTO(
    @NotBlank String input,
    @NotBlank String inputFormat,
    @NotBlank String outputFormat,
    String width,
    String height) {}
