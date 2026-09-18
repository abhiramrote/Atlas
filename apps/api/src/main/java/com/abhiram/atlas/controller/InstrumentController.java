package com.abhiram.atlas.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiram.atlas.dto.InstrumentResponse;
import com.abhiram.atlas.service.InstrumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService service;

    @GetMapping
    public List<InstrumentResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public InstrumentResponse getById(
            @PathVariable UUID id) {

        return service.getById(id);
    }
    @GetMapping("/symbol/{symbol}")
    public InstrumentResponse getBySymbol(
        @PathVariable String symbol) {

        return service.getBySymbol(symbol);
    }
}