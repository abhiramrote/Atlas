package com.abhiram.atlas.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiram.atlas.dto.FinancialMetricsResponse;
import com.abhiram.atlas.service.MetricsService;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService service;

    public MetricsController(MetricsService service) {
        this.service = service;
    }

    @GetMapping("/company/{companyId}")
    public FinancialMetricsResponse getMetrics(
            @PathVariable UUID companyId) {

        return service.getMetrics(companyId);
    }
}