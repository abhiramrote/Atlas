package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.PriceBarResponse;
import com.abhiram.atlas.service.PriceHistoryService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
public class PriceHistoryController {

    private final PriceHistoryService service;

    public PriceHistoryController(
            PriceHistoryService service) {
        this.service = service;
    }

    @GetMapping("/{instrumentId}")
    public List<PriceBarResponse> getPriceHistory(
            @PathVariable UUID instrumentId) {

        return service.getPriceHistory(instrumentId);
    }
}