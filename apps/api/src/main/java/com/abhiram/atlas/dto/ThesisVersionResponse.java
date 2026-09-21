package com.abhiram.atlas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ThesisVersionResponse(
        UUID id,
        Integer versionNumber,
        Integer horizonMonths,
        String conviction,
        String rationale,
        String keyRisks,
        Integer snapshotFundamentalScore,
        Integer snapshotTechnicalScore,
        Integer snapshotFinalScore,
        String snapshotRating,
        String snapshotPolicyVersion,
        BigDecimal snapshotClosePrice,
        LocalDateTime publishedAt,
        LocalDateTime supersededAt,
        boolean superseded,
        List<ConditionResponse> invalidationConditions
) {
}
