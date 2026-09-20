package com.abhiram.atlas.controller;

import com.abhiram.atlas.service.PriceHistoryIngestionService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/prices")
public class PriceRefreshController {

    private final PriceHistoryIngestionService service;

    public PriceRefreshController(
            PriceHistoryIngestionService service) {

        this.service = service;
    }

    @PostMapping("/refresh/{symbol}")
    public String refresh(
            @PathVariable String symbol) {

        service.ingestHistory(symbol);

        return "Prices refreshed successfully for " + symbol;
    }
}