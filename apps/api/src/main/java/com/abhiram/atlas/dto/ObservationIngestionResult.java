package com.abhiram.atlas.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Outcome of recording provider figures as observations.
 *
 * restatements counts periods where the provider reported a figure
 * that differs from what Atlas previously knew. A non-zero count is
 * significant: it means historical scores computed before this
 * ingest were based on different numbers.
 */
public record ObservationIngestionResult(
        String symbol,
        String status,
        Integer periodsRecorded,
        Integer newPeriods,
        Integer restatements,
        Integer unchanged,
        LocalDate knowledgeDate,
        String provider,
        List<String> restatementDetails,
        String message
) {
}
