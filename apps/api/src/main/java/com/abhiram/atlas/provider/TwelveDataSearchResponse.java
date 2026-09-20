package com.abhiram.atlas.provider;

import java.util.List;

public record TwelveDataSearchResponse(
        List<TwelveDataSymbol> data,
        String status
) {
}