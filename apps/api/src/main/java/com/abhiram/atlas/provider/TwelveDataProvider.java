package com.abhiram.atlas.provider;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.abhiram.atlas.config.TwelveDataConfig;

@Component
public class TwelveDataProvider {

    private final RestClient restClient;
    private final TwelveDataConfig config;

    public TwelveDataProvider(
            RestClient restClient,
            TwelveDataConfig config) {

        this.restClient = restClient;
        this.config = config;
    }

    public String fetchPrice(String symbol) {

        String url =
                "https://api.twelvedata.com/price"
                + "?symbol=" + symbol
                + "&exchange=NSE"
                + "&apikey=" + config.getApiKey();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    public TwelveDataTimeSeriesResponse fetchPriceHistory(
            String symbol) {

        String url =
                "https://api.twelvedata.com/time_series"
                + "?symbol=" + symbol
                + "&exchange=NSE"
                + "&interval=1day"
                + "&outputsize=30"
                + "&apikey=" + config.getApiKey();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TwelveDataTimeSeriesResponse.class);
    }

    public TwelveDataSearchResponse fetchProfile(
            String symbol) {

        String url =
                "https://api.twelvedata.com/symbol_search"
                + "?symbol=" + symbol
                + "&apikey=" + config.getApiKey();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(TwelveDataSearchResponse.class);
    }

    public CompanyProfileData getCompanyProfile(
            String symbol) {

        TwelveDataSearchResponse response =
                fetchProfile(symbol);

        TwelveDataSymbol bestMatch =
                response.data()
                        .stream()
                        .filter(item ->
                                item.symbol()
                                        .equalsIgnoreCase(symbol))
                        .findFirst()
                        .orElseThrow();

        return new CompanyProfileData(
                bestMatch.symbol(),
                bestMatch.instrument_name(),
                "UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                "Imported from Twelve Data",
                "TWELVE_DATA"
        );
    }
}