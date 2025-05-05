package com.researchspace.chemistry.extract;

import jakarta.validation.constraints.NotBlank;

public record ExtractionRequest(
    @NotBlank(message = "input parameter must be present and not null or an empty string.")
        String input) {}
