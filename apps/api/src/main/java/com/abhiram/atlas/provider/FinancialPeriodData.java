package com.abhiram.atlas.provider;

import java.math.BigDecimal;

/**
 * Provider-neutral financial period.
 *
 * This is the contract between any fundamentals provider and Atlas
 * ingestion. Adding a second provider later means mapping into this
 * record, not changing the ingestion service.
 *
 * Nulls are meaningful. A null revenue means the provider did not
 * report it, which is different from reporting zero.
 */
public record FinancialPeriodData(
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal operatingProfit,
        BigDecimal operatingCashFlow
) {

    /**
     * A period is usable only if it can be identified and carries at
     * least one figure. Storing an empty period would pollute growth
     * calculations with meaningless rows.
     */
    public boolean isUsable() {
        return fiscalYear != null
                && (revenue != null
                    || netIncome != null
                    || operatingProfit != null
                    || operatingCashFlow != null);
    }
}