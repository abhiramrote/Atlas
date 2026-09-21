package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.RiskMetricsResponse;
import com.abhiram.atlas.service.RiskMetricsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/risk")
public class RiskMetricsController {

    private final RiskMetricsService service;

    public RiskMetricsController(RiskMetricsService service) {
        this.service = service;
    }

    @GetMapping("/{instrumentId}")
    public RiskMetricsResponse getRiskMetrics(
            @PathVariable UUID instrumentId
    ) {
        return service.getRiskMetrics(instrumentId);
    }
}
