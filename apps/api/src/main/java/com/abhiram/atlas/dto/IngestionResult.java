package com.abhiram.atlas.dto;

public record IngestionResult(
        String symbol,
        String status,
        String message
) {
}