package com.abhiram.atlas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConditionRequest(

        @NotBlank(message = "Metric is required")
        String metric,

        @NotBlank(message = "Comparison is required")
        String comparison,

        @NotNull(message = "Threshold is required")
        BigDecimal threshold,

        String description
) {
}
