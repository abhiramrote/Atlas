package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.DataQualityReport;
import com.abhiram.atlas.service.DataQualityService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/data-quality")
public class DataQualityController {

    private final DataQualityService service;

    public DataQualityController(DataQualityService service) {
        this.service = service;
    }

    /**
     * Reports data quality issues for one company.
     *
     * Read reliableForScoring before trusting any opportunity score
     * for this company.
     */
    @GetMapping("/companies/{companyId}")
    public DataQualityReport check(
            @PathVariable UUID companyId
    ) {
        return service.check(companyId);
    }
}
