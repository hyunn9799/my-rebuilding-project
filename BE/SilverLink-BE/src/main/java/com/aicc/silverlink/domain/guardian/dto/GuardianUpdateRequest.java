package com.aicc.silverlink.domain.guardian.dto;

import jakarta.validation.constraints.NotBlank;

public record GuardianUpdateRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String email,
        String addressLine1,
        String addressLine2,
        String zipcode
) {}