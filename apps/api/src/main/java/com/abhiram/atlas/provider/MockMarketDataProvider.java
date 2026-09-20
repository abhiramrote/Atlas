package com.abhiram.atlas.provider;

import org.springframework.stereotype.Component;

@Component
public class MockMarketDataProvider
        implements MarketDataProvider {

    @Override
    public String getProviderName() {
        return "MOCK_PROVIDER";
    }

    @Override
    public String fetchCompanyData(
            String symbol) {

        return """
                {
                    "symbol":"%s",
                    "sector":"Technology"
                }
                """.formatted(symbol);
    }
}