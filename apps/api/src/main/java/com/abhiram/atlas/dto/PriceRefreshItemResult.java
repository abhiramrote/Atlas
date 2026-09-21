package com.abhiram.atlas.dto;

public record PriceRefreshItemResult(
        String symbol,
        String status,
        Integer barsImported,
        String message
) {
}