package com.abhiram.atlas.dto;

import java.math.BigDecimal;

public record MomentumResponse(
        String symbol,
        BigDecimal firstClose,
        BigDecimal latestClose,
        BigDecimal priceChangePercent,
        String trend
) {
}