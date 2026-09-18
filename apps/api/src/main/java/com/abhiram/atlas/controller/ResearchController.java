package com.abhiram.atlas.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.abhiram.atlas.dto.ResearchSummaryResponse;
import com.abhiram.atlas.service.ResearchService;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final ResearchService service;

    public ResearchController(
            ResearchService service) {
        this.service = service;
    }

    @GetMapping("/company/{companyId}")
    public ResearchSummaryResponse getSummary(
            @PathVariable UUID companyId) {

        return service.getSummary(companyId);
    }
}