package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialGrowthResponse(
        UUID companyId,
        String symbol,
        Integer currentFiscalYear,
        Integer previousFiscalYear,
        BigDecimal currentRevenue,
        BigDecimal previousRevenue,
        BigDecimal revenueGrowthPercent,
        BigDecimal netIncomeGrowthPercent,
        BigDecimal operatingProfitGrowthPercent,
        BigDecimal operatingCashFlowGrowthPercent
) {
}