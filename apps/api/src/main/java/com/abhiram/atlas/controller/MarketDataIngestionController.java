package com.abhiram.atlas.controller;

import com.abhiram.atlas.service.MarketDataIngestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ingestion")
public class MarketDataIngestionController {

    private final MarketDataIngestionService service;

    public MarketDataIngestionController(
            MarketDataIngestionService service) {
        this.service = service;
    }

    @PostMapping("/{symbol}")
    public String ingest(
            @PathVariable String symbol) {

        return service.ingestCompany(symbol);
    }
}