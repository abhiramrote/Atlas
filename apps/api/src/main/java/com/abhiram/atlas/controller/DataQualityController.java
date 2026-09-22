package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.DataQualityOverview;
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
     * Quality status across the scoreable universe.
     *
     * Returns only enough for a list view to decide whether to show
     * a warning. Full issue detail lives on the per company
     * endpoint.
     */
    @GetMapping("/overview")
    public DataQualityOverview overview() {
        return service.checkAll();
    }

    /**
     * Full report for one company.
     *
     * Read reliableForScoring before trusting any opportunity score.
     */
    @GetMapping("/companies/{companyId}")
    public DataQualityReport check(
            @PathVariable UUID companyId
    ) {
        return service.check(companyId);
    }
}
