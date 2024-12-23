package com.github.rspaceos.chemistry.convert;

import jakarta.validation.constraints.NotNull;

public record ConvertDTO(@NotNull String input, @NotNull String outputFormat) {}
