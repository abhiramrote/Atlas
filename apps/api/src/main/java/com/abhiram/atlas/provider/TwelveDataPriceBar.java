package com.abhiram.atlas.provider;

public record TwelveDataPriceBar(
        String datetime,
        String open,
        String high,
        String low,
        String close,
        String volume
) {
}
