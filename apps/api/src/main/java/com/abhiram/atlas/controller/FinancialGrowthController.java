package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.FinancialGrowthResponse;
import com.abhiram.atlas.service.FinancialGrowthService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/growth")
public class FinancialGrowthController {

    private final FinancialGrowthService service;

    public FinancialGrowthController(
            FinancialGrowthService service
    ) {
        this.service = service;
    }

    @GetMapping("/company/{companyId}")
    public FinancialGrowthResponse getGrowth(
            @PathVariable UUID companyId
    ) {
        return service.getGrowth(companyId);
    }
}