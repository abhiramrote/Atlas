package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.PriceRefreshAllResult;
import com.abhiram.atlas.dto.PriceRefreshItemResult;
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
            PriceHistoryIngestionService service
    ) {
        this.service = service;
    }

    @PostMapping("/refresh/{symbol}")
    public PriceRefreshItemResult refresh(
            @PathVariable String symbol
    ) {
        return service.ingestHistory(symbol);
    }

    @PostMapping("/refresh-all")
    public PriceRefreshAllResult refreshAll() {
        return service.refreshAll();
    }
}