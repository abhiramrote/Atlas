package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceBarResponse(
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume
) {
}
