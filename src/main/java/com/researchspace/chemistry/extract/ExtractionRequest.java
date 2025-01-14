package com.researchspace.chemistry.extract;

import jakarta.validation.constraints.NotNull;

public record ExtractionRequest(@NotNull String input) {}
