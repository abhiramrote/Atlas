package com.abhiram.atlas.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.service.InstrumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {
    private final InstrumentService service;
    @GetMapping
    public List<Instrument> getAll() {

    return service.getAll();
}
}
