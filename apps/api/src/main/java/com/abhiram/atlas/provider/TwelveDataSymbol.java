package com.abhiram.atlas.provider;

public record TwelveDataSymbol(
        String symbol,
        String instrument_name,
        String exchange,
        String country,
        String currency
) {
}