package com.abhiram.atlas.dto;

import java.util.List;
import java.util.UUID;

public record OpportunityScoreResponse(
        UUID companyId,
        String symbol,
        String companyName,
        Integer score,
        Integer maximumScore,
        String rating,
        String policyVersion,
        Integer currentFiscalYear,
        Integer previousFiscalYear,
        List<ScoreFactorResponse> factors
) {
}