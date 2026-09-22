package com.abhiram.atlas.dto;

import java.util.List;
import java.util.UUID;

/**
 * Data quality assessment for a company's financial history.
 *
 * reliableForScoring is the field callers should branch on. It is
 * false when any ERROR level issue affects a period that scoring
 * would use, because a confident score computed from corrupted input
 * is worse than no score at all.
 */
public record DataQualityReport(
        UUID companyId,
        String symbol,
        Integer periodsChecked,
        Integer issueCount,
        Integer errorCount,
        Boolean reliableForScoring,
        String summary,
        List<DataQualityIssue> issues
) {
}
