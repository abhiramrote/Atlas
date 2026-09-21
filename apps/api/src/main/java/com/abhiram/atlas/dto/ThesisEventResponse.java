package com.abhiram.atlas.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThesisEventResponse(
        UUID id,
        String eventType,
        String fromState,
        String toState,
        Integer versionNumber,
        String detail,
        LocalDateTime occurredAt
) {
}
