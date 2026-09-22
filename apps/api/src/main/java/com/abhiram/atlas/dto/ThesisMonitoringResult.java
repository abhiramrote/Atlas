package com.abhiram.atlas.dto;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of monitoring a single thesis.
 *
 * conditionsEvaluated counts only conditions where a current value
 * could actually be computed. Conditions skipped for missing data are
 * excluded, so a low count signals a data gap rather than a healthy
 * thesis.
 */
public record ThesisMonitoringResult(
        UUID thesisId,
        String title,
        String stateBefore,
        String stateAfter,
        Integer conditionsEvaluated,
        Integer newBreaches,
        List<String> breachDetails,
        String note
) {
}
