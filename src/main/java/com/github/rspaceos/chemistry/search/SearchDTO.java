package com.github.rspaceos.chemistry.search;

import jakarta.validation.constraints.NotNull;

public record SearchDTO(@NotNull String chemicalSearchTerm) {}
