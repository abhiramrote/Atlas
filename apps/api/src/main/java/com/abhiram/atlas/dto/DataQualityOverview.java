package com.abhiram.atlas.dto;

import java.util.List;

/**
 * Quality status across the scoreable universe.
 *
 * companiesChecked counts only scoreable companies. Banks are
 * excluded because a quality assessment of figures Atlas refuses to
 * interpret would be meaningless.
 */
public record DataQualityOverview(
        Integer companiesChecked,
        Integer unreliableCount,
        List<DataQualitySummary> companies
) {
}
