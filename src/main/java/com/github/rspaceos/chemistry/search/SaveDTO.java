package com.github.rspaceos.chemistry.search;

import jakarta.validation.constraints.NotNull;

public record SaveDTO(@NotNull String chemical, @NotNull String chemicalId) {}
