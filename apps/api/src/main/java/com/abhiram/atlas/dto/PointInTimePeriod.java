package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One reporting period as known on the evaluation date.
 *
 * Both dates are exposed deliberately. The gap between periodEndDate
 * and knowledgeDate is the reporting lag, and seeing it makes
 * lookahead bias obvious rather than hidden.
 */
public record PointInTimePeriod(
        Integer fiscalYear,
        Integer fiscalQuarter,
        LocalDate periodEndDate,
        LocalDate knowledgeDate,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal operatingProfit,
        BigDecimal operatingCashFlow,
        BigDecimal operatingMarginPercent,
        BigDecimal profitMarginPercent,
        Integer revisionNumber,
        String sourceProvider,
        String unit
) {
}