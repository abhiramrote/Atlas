package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.ScoreabilityResponse;
import com.abhiram.atlas.dto.SectorBaseline;
import com.abhiram.atlas.dto.SectorComparison;
import com.abhiram.atlas.service.SectorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService service;

    public SectorController(SectorService service) {
        this.service = service;
    }

    @GetMapping("/baselines")
    public List<SectorBaseline> getBaselines() {
        return service.getBaselines();
    }

    @GetMapping("/companies/{companyId}/scoreability")
    public ScoreabilityResponse getScoreability(
            @PathVariable UUID companyId
    ) {
        return service.getScoreability(companyId);
    }

    @GetMapping("/companies/{companyId}/comparison")
    public SectorComparison compareToSector(
            @PathVariable UUID companyId
    ) {
        return service.compareToSector(companyId);
    }
}
