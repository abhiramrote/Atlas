package com.abhiram.atlas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request to publish a thesis.
 *
 * At least one invalidation condition is mandatory. A thesis that
 * cannot be proven wrong is not a thesis, and permitting one would
 * defeat the purpose of the entire feature.
 */
public record PublishThesisRequest(

        @NotNull(message = "Company is required")
        UUID companyId,

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @NotNull(message = "Horizon is required")
        @Min(value = 1, message = "Horizon must be at least 1 month")
        @Max(value = 120, message = "Horizon must be 120 months or less")
        Integer horizonMonths,

        @NotBlank(message = "Conviction is required")
        String conviction,

        @NotBlank(message = "Rationale is required")
        @Size(min = 40, message =
                "Rationale must be at least 40 characters")
        String rationale,

        String keyRisks,

        @NotEmpty(message =
                "At least one invalidation condition is required")
        @Valid
        List<ConditionRequest> invalidationConditions
) {
}