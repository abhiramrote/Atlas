package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.ObservationIngestionResult;
import com.abhiram.atlas.service.ObservationIngestionService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/observations")
public class ObservationController {

    private final ObservationIngestionService service;

    public ObservationController(
            ObservationIngestionService service
    ) {
        this.service = service;
    }

    /**
     * Records current provider figures as dated observations.
     *
     * The knowledge date is always today. It is not settable through
     * this endpoint, because backdating an observation to before it
     * was actually knowable would reintroduce the lookahead bias the
     * whole design exists to prevent.
     */
    @PostMapping("/ingest/{symbol}")
    public ObservationIngestionResult ingest(
            @PathVariable String symbol
    ) {
        return service.ingest(symbol);
    }
}
