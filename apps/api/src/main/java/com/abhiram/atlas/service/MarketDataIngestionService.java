package com.abhiram.atlas.service;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.IngestionResult;
import com.abhiram.atlas.provider.MarketDataProvider;

@Service
public class MarketDataIngestionService {

    private final MarketDataProvider provider;

    public MarketDataIngestionService(
            MarketDataProvider provider) {

        this.provider = provider;
    }

    public IngestionResult ingestCompany(String symbol) {

        String data =
                provider.fetchCompanyData(symbol);

        return new IngestionResult(
                symbol.toUpperCase(),
                "SUCCESS",
                data
        );
    }
}