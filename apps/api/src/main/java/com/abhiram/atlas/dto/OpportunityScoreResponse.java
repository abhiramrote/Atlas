package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record OpportunityScoreResponse(
        String symbol,
        String companyName,
        BigDecimal profitMargin,
        BigDecimal revenueMargin,
        Integer score,
        String rating
) {
}