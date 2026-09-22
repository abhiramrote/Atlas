package com.abhiram.atlas.dto;

import java.math.BigDecimal;

/**
 * A specific data quality concern for one reporting period.
 *
 * observedValue and expectedRange are both included so the reader can
 * judge the finding rather than trusting the check. A validation that
 * only says "suspicious" is much less useful than one that shows the
 * number and the band it fell outside.
 */
public record DataQualityIssue(
        String check,
        String severity,
        Integer fiscalYear,
        String metric,
        BigDecimal observedValue,
        String expectedRange,
        String explanation
) {
}