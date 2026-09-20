package com.abhiram.atlas.dto;

import java.time.LocalDateTime;

public record IngestionResult(
        String symbol,
        String provider,
        String status,
        String message,
        LocalDateTime completedAt
) {
}