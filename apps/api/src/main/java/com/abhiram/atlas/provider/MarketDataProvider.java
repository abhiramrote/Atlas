package com.abhiram.atlas.provider;

public interface MarketDataProvider {

    String getProviderName();

    CompanyProfileData fetchCompanyData(String symbol);
}