package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record TechnicalScoreResponse(
        String symbol,
        BigDecimal momentumPercent,
        Integer technicalScore
) {
}