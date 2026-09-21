package com.abhiram.atlas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviseThesisRequest(

        @NotNull(message = "Horizon is required")
        @Min(1)
        @Max(120)
        Integer horizonMonths,

        @NotBlank(message = "Conviction is required")
        String conviction,

        @NotBlank(message = "Rationale is required")
        @Size(min = 40, message =
                "Rationale must be at least 40 characters")
        String rationale,

        String keyRisks,

        @NotBlank(message =
                "A reason for the revision is required")
        String revisionReason,

        @NotEmpty(message =
                "At least one invalidation condition is required")
        @Valid
        List<ConditionRequest> invalidationConditions
) {
}

