package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Risk characteristics derived from stored daily price bars.
 *
 * All values are backward looking and describe only the period covered
 * by the stored history. They do not predict future risk.
 *
 * annualisedVolatilityPercent uses the standard deviation of daily
 * returns scaled by the square root of 252 trading days. With a short
 * history this estimate is noisy, which is why observationDays is
 * reported alongside it.
 *
 * maximumDrawdownPercent is the largest peak to trough decline in the
 * stored window, expressed as a negative percentage.
 */
public record RiskMetricsResponse(
        UUID instrumentId,
        String symbol,
        Integer observationDays,
        BigDecimal annualisedVolatilityPercent,
        BigDecimal maximumDrawdownPercent,
        BigDecimal peakClose,
        BigDecimal troughClose,
        String riskBand,
        String note
) {
}
