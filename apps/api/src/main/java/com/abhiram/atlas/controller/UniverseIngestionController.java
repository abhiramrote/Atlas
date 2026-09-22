package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.BulkIngestionResult;
import com.abhiram.atlas.service.UniverseIngestionService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/universe")
public class UniverseIngestionController {

    private final UniverseIngestionService service;

    public UniverseIngestionController(
            UniverseIngestionService service
    ) {
        this.service = service;
    }

    /**
     * Ingests fundamentals for every active instrument.
     *
     * This is a long running call. With twenty instruments and paced
     * requests it takes roughly forty seconds, so use a client that
     * will not time out early.
     */
    @PostMapping("/ingest")
    public BulkIngestionResult ingestUniverse() {
        return service.ingestUniverse();
    }
}
