package com.abhiram.atlas.service;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.IngestionResult;

@Service
public class MarketDataIngestionService {

    public IngestionResult ingestCompany(String symbol) {

        return new IngestionResult(
        symbol.toUpperCase(),
        "SUCCESS",
        "Atlas ingestion pipeline accepted request"
        );
    }
}