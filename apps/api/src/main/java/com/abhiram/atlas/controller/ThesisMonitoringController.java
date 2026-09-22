package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.MonitoringRunResult;
import com.abhiram.atlas.dto.ThesisMonitoringResult;
import com.abhiram.atlas.service.ThesisMonitoringService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/monitoring")
public class ThesisMonitoringController {

    private final ThesisMonitoringService service;

    public ThesisMonitoringController(
            ThesisMonitoringService service
    ) {
        this.service = service;
    }

    @PostMapping("/theses")
    public MonitoringRunResult monitorAll() {
        return service.monitorAll();
    }

    @PostMapping("/theses/{thesisId}")
    public ThesisMonitoringResult monitorOne(
            @PathVariable UUID thesisId
    ) {
        return service.monitorThesis(thesisId);
    }
}
