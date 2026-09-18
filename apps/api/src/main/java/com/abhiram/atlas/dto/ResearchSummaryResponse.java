package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record ResearchSummaryResponse(
        String symbol,
        String companyName,
        String sector,

        BigDecimal revenue,
        BigDecimal netIncome,

        BigDecimal revenueMargin,
        BigDecimal profitMargin
) {
}