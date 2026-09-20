package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.OpportunityScoreResponse;
import com.abhiram.atlas.service.OpportunityScoreService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityScoreController {

    private final OpportunityScoreService service;

    public OpportunityScoreController(
            OpportunityScoreService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<OpportunityScoreResponse> getRankedOpportunities(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.getRankedOpportunities(limit);
    }

    @GetMapping("/company/{companyId}")
    public OpportunityScoreResponse getOpportunity(
            @PathVariable UUID companyId
    ) {
        return service.scoreCompany(companyId);
    }
}