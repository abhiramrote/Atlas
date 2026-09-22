package com.abhiram.atlas.dto;

import java.util.List;

public record MonitoringRunResult(
        Integer thesesMonitored,
        Integer thesesWithNewBreaches,
        Integer thesesInvalidated,
        List<ThesisMonitoringResult> results
) {
}
