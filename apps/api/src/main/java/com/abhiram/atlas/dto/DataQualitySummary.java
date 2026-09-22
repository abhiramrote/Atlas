package com.abhiram.atlas.dto;

import java.util.UUID;

/**
 * Minimal quality status for one company.
 *
 * Deliberately narrow. The dashboard needs to know whether to show a
 * warning badge, not what every issue was. Returning full issue lists
 * for fifty companies would make the payload large for no benefit,
 * since the detail page already exists for that.
 */
public record DataQualitySummary(
        UUID companyId,
        String symbol,
        Boolean reliableForScoring,
        Integer errorCount
) {
}
