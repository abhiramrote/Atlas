package com.abhiram.atlas.provider;

import org.springframework.stereotype.Component;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

    @Override
    public String getProviderName() {
        return "MOCK_PROVIDER";
    }

    @Override
    public CompanyProfileData fetchCompanyData(String symbol) {

        String normalizedSymbol = symbol.trim().toUpperCase();

        return switch (normalizedSymbol) {
            case "RELIANCE" -> new CompanyProfileData(
                    "RELIANCE",
                    "Reliance Industries Ltd",
                    "NSE",
                    "Energy",
                    "Diversified Energy and Consumer Businesses",
                    "https://www.ril.com",
                    "Reliance Industries company profile."
            );

            case "TCS" -> new CompanyProfileData(
                    "TCS",
                    "Tata Consultancy Services Ltd",
                    "NSE",
                    "Information Technology",
                    "IT Services and Consulting",
                    "https://www.tcs.com",
                    "Tata Consultancy Services company profile."
            );

            case "INFY" -> new CompanyProfileData(
                    "INFY",
                    "Infosys Ltd",
                    "NSE",
                    "Information Technology",
                    "IT Services and Consulting",
                    "https://www.infosys.com",
                    "Infosys company profile."
            );

            default -> throw new IllegalArgumentException(
                    "Mock provider does not contain symbol: "
                            + normalizedSymbol
            );
        };
    }
}
