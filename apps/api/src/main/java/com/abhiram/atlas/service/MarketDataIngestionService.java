package com.abhiram.atlas.service;

import org.springframework.stereotype.Service;

@Service
public class MarketDataIngestionService {

    public String ingestCompany(String symbol) {

        // TODO:
        // Call real market provider
        // Parse response
        // Store in database

        return "Ingestion placeholder for " + symbol;
    }
}