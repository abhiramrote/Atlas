package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.MomentumResponse;
import com.abhiram.atlas.service.MomentumService;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/momentum")
public class MomentumController {

    private final MomentumService service;

    public MomentumController(
            MomentumService service) {
        this.service = service;
    }

    @GetMapping("/{instrumentId}")
    public MomentumResponse getMomentum(
            @PathVariable UUID instrumentId) {

        return service.getMomentum(instrumentId);
    }
}