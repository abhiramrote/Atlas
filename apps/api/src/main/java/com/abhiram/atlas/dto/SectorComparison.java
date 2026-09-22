package com.abhiram.atlas.dto;

import java.math.BigDecimal;

/**
 * How a company's margins compare with its sector.
 *
 * relativeOperatingMargin is a percentage difference from the sector
 * median, not a percentage point difference. A value of 25 means the
 * company's margin is 25 percent higher than the median, not 25
 * points higher.
 */
public record SectorComparison(
        String sector,
        Integer sectorPeerCount,
        Boolean baselineSufficient,
        BigDecimal companyOperatingMargin,
        BigDecimal sectorMedianOperatingMargin,
        BigDecimal relativeOperatingMargin,
        BigDecimal companyProfitMargin,
        BigDecimal sectorMedianProfitMargin,
        BigDecimal relativeProfitMargin,
        String note
) {
}