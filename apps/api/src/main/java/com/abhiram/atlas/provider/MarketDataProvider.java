package com.abhiram.atlas.provider;

public interface MarketDataProvider {

    String getProviderName();

    String fetchCompanyData(String symbol);

}