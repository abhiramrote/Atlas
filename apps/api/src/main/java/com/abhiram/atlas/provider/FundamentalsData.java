package com.abhiram.atlas.provider;

import java.util.List;

/**
 * Provider-neutral company fundamentals.
 */
public record FundamentalsData(
        String symbol,
        String companyName,
        String sector,
        String industry,
        String description,
        List<FinancialPeriodData> periods,
        String providerName
) {
}
