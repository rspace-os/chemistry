package com.researchspace.chemistry.bondstrength;

import jakarta.validation.constraints.NotBlank;

public record BondStrengthRequest(
    @NotBlank(message = "input parameter must be present and not null or an empty string.")
        String input) {}
