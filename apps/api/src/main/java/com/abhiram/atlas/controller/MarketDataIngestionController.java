package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.IngestionResult;
import com.abhiram.atlas.service.MarketDataIngestionService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ingestion")
public class MarketDataIngestionController {

    private final MarketDataIngestionService service;

    public MarketDataIngestionController(
            MarketDataIngestionService service
    ) {
        this.service = service;
    }

    @PostMapping("/{symbol}")
    public IngestionResult ingest(
            @PathVariable String symbol
    ) {
        return service.ingestCompany(symbol);
    }
}