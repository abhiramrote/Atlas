package com.abhiram.atlas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api")
    public Map<String, String> home() {
        return Map.of(
                "application", "Atlas",
                "status", "UP"
        );
    }
}