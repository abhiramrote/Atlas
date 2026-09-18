package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record FinancialStatementResponse(
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal operatingProfit,
        BigDecimal operatingCashFlow
) {
}