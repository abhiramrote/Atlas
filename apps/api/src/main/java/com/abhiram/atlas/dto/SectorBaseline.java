package com.abhiram.atlas.dto;

import java.math.BigDecimal;

/**
 * Median metrics for a sector.
 *
 * peerCount and sufficient are exposed deliberately. A baseline drawn
 * from one or two companies is not a baseline, and a caller that
 * cannot see the peer count would treat a meaningless median as
 * authoritative.
 */
public record SectorBaseline(
        String sector,
        Integer peerCount,
        Boolean sufficient,
        BigDecimal medianOperatingMargin,
        BigDecimal medianProfitMargin,
        String note
) {
}
