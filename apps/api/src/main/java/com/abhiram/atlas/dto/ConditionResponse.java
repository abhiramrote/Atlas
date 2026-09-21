package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConditionResponse(
        UUID id,
        String metric,
        String comparison,
        BigDecimal threshold,
        String description,
        boolean breached,
        LocalDateTime breachedAt,
        BigDecimal breachedValue
) {
}