package com.researchspace.chemistry.image;

import jakarta.validation.constraints.NotNull;

public record ImageDTO(
    @NotNull String input, @NotNull String outputFormat, String width, String height) {}
