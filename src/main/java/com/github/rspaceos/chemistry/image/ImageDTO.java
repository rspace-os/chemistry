package com.github.rspaceos.chemistry.image;

import jakarta.validation.constraints.NotNull;

public record ImageDTO(@NotNull String input, @NotNull String outputFormat) {}
