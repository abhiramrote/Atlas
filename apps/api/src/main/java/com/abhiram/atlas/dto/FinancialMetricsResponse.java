package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record FinancialMetricsResponse(
        Integer fiscalYear,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal operatingProfit,
        BigDecimal revenueMargin,
        BigDecimal profitMargin
) {
}