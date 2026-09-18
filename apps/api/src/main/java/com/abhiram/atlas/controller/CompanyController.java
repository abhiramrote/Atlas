package com.abhiram.atlas.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiram.atlas.dto.CompanyResponse;
import com.abhiram.atlas.service.CompanyService;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    public List<CompanyResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(
            @PathVariable UUID id) {

        return service.getById(id);
    }

    @GetMapping("/by-instrument/{instrumentId}")
    public CompanyResponse getByInstrumentId(
            @PathVariable UUID instrumentId) {

        return service.getByInstrumentId(instrumentId);
    }
}