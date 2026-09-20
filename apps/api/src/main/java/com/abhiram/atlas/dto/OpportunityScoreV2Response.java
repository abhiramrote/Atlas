package com.abhiram.atlas.dto;

public record OpportunityScoreV2Response(
        String symbol,
        Integer fundamentalScore,
        Integer technicalScore,
        Integer finalScore,
        String rating
) {
}