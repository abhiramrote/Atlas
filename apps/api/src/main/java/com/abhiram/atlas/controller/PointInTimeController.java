package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.PointInTimeSnapshot;
import com.abhiram.atlas.service.PointInTimeService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/point-in-time")
public class PointInTimeController {

    private final PointInTimeService service;

    public PointInTimeController(PointInTimeService service) {
        this.service = service;
    }

    /**
     * Returns what Atlas knew about a company on a given date.
     *
     * Example:
     *   GET /api/point-in-time/companies/{id}?asOf=2024-06-30
     */
    @GetMapping("/companies/{companyId}")
    public PointInTimeSnapshot getSnapshot(
            @PathVariable UUID companyId,

            @RequestParam(name = "asOf")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOfDate
    ) {
        return service.getSnapshot(companyId, asOfDate);
    }
}