package com.abhiram.atlas.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What Atlas knew about a company on a specific date.
 *
 * dataQualityNote is not decoration. A snapshot assembled from
 * backfilled rows has an approximated knowledge date and cannot
 * support honest evaluation, and the caller needs to know that
 * before drawing conclusions.
 */
public record PointInTimeSnapshot(
        UUID companyId,
        String symbol,
        String companyName,
        LocalDate asOfDate,
        Integer periodsKnown,
        LocalDate earliestKnowledgeDate,
        String dataQualityNote,
        List<PointInTimePeriod> periods
) {
}