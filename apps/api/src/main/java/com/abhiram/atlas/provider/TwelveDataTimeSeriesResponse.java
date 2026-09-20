package com.abhiram.atlas.provider;

import java.util.List;

public record TwelveDataTimeSeriesResponse(
        List<TwelveDataPriceBar> values,
        String status
) {
}