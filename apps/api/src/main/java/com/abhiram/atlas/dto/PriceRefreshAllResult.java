package com.abhiram.atlas.dto;

import java.util.List;

public record PriceRefreshAllResult(
        Integer total,
        Integer succeeded,
        Integer failed,
        List<PriceRefreshItemResult> results
) {
}