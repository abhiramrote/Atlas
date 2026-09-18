package com.abhiram.atlas.controller;

import org.springframework.web.bind.annotation.*;

import com.abhiram.atlas.dto.IngestionResult;
import com.abhiram.atlas.service.MarketDataIngestionService;

@RestController
@RequestMapping("/api/admin/ingestion")
public class MarketDataIngestionController {

    private final MarketDataIngestionService service;

    public MarketDataIngestionController(
            MarketDataIngestionService service) {
        this.service = service;
    }

    @PostMapping("/{symbol}")
    public IngestionResult ingest(
            @PathVariable String symbol) {

        return service.ingestCompany(symbol);
    }
}