package com.abhiram.atlas.dto;

/**
 * Per symbol outcome from a bulk run.
 *
 * observationsRecorded and restatements are reported separately from
 * periodsImported because they answer different questions.
 * periodsImported says what scoring will use now. restatements says
 * whether previously computed scores were based on different figures.
 */
public record BulkIngestionItem(
        String symbol,
        String status,
        Integer periodsImported,
        Integer observationsRecorded,
        Integer restatements,
        String message
) {
}