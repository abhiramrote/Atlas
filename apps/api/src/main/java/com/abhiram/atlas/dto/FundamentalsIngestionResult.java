package com.abhiram.atlas.dto;

/**
 * Outcome of a fundamentals ingestion.
 *
 * periodsReplaced reports how many existing rows were removed. This
 * matters because ingestion is destructive by design, and the caller
 * should be able to see what was discarded.
 */
public record FundamentalsIngestionResult(
        String symbol,
        String status,
        Integer periodsImported,
        Integer periodsReplaced,
        String provider,
        String message
) {
}
