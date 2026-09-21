package com.abhiram.atlas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ThesisResponse(
        UUID id,
        UUID companyId,
        String symbol,
        String companyName,
        String title,
        String state,
        Integer currentVersion,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDate horizonEndsOn,
        List<String> allowedTransitions,
        ThesisVersionResponse currentVersionDetail
) {
}
