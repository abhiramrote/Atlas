package com.abhiram.atlas.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.service.OpportunityScoreV2Service;

@RestController
@RequestMapping("/api/opportunities/v2")
public class OpportunityScoreV2Controller {

    private final OpportunityScoreV2Service service;

    public OpportunityScoreV2Controller(
            OpportunityScoreV2Service service) {

        this.service = service;
    }

    @GetMapping("/{companyId}")
    public OpportunityScoreV2Response getScore(
            @PathVariable UUID companyId) {

        return service.score(companyId);
    }
}