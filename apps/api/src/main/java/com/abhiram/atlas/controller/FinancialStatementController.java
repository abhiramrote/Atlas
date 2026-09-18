package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.FinancialStatementResponse;
import com.abhiram.atlas.service.FinancialStatementService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial-statements")
public class FinancialStatementController {

    private final FinancialStatementService service;

    public FinancialStatementController(
            FinancialStatementService service) {
        this.service = service;
    }

    @GetMapping("/company/{companyId}")
    public List<FinancialStatementResponse>
    getByCompanyId(
            @PathVariable UUID companyId) {

        return service.getByCompanyId(companyId);
    }
}