package com.abhiram.atlas.dto;

import java.util.UUID;

/**
 * Whether Atlas can meaningfully score a company.
 *
 * This is returned instead of a score for unscoreable companies. An
 * explicit refusal with a reason is more useful than a low number
 * that looks like a judgement.
 */
public record ScoreabilityResponse(
        UUID companyId,
        String symbol,
        String companyName,
        String scoringProfile,
        Boolean scoreable,
        String reason
) {
}
