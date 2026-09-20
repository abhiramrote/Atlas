package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record ScoreFactorResponse(
        String name,
        BigDecimal value,
        Integer pointsAwarded,
        Integer maximumPoints,
        String explanation
) {
}