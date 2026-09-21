package com.abhiram.atlas.dto;

import java.util.UUID;

/**
 * Combined opportunity score.
 *
 * Fundamental and technical components are reported separately so a
 * partially available result is still useful. When price history is
 * missing, technicalScore is null rather than zero, because zero would
 * incorrectly imply a measured value of no momentum.
 */
public record OpportunityScoreV2Response(
        UUID companyId,
        String symbol,
        String companyName,
        Integer fundamentalScore,
        Integer technicalScore,
        Integer finalScore,
        Integer maximumScore,
        String rating,
        String policyVersion,
        boolean technicalScoreAvailable,
        String technicalScoreNote
) {
}
