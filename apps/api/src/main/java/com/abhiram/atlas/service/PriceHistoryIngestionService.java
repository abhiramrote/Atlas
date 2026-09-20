package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.PriceRefreshAllResult;
import com.abhiram.atlas.dto.PriceRefreshItemResult;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.provider.TwelveDataPriceBar;
import com.abhiram.atlas.provider.TwelveDataProvider;
import com.abhiram.atlas.provider.TwelveDataTimeSeriesResponse;
import com.abhiram.atlas.repository.InstrumentRepository;
import com.abhiram.atlas.repository.PriceBarRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PriceHistoryIngestionService {

    private final TwelveDataProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;

    public PriceHistoryIngestionService(
            TwelveDataProvider provider,
            InstrumentRepository instrumentRepository,
            PriceBarRepository priceBarRepository
    ) {
        this.provider = provider;
        this.instrumentRepository = instrumentRepository;
        this.priceBarRepository = priceBarRepository;
    }

    @Transactional
    public PriceRefreshItemResult ingestHistory(String symbol) {

        String normalizedSymbol = symbol.trim().toUpperCase();

        Instrument instrument = instrumentRepository
                .findBySymbolIgnoreCase(normalizedSymbol)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instrument not found: " + normalizedSymbol
                        )
                );

        TwelveDataTimeSeriesResponse response =
                provider.fetchPriceHistory(normalizedSymbol);

        if (response == null
                || response.values() == null
                || response.values().isEmpty()) {

            throw new IllegalArgumentException(
                    "No price history returned for: " + normalizedSymbol
            );
        }

        List<PriceBar> entities = response.values()
                .stream()
                .map(bar -> toEntity(instrument, bar))
                .toList();

        priceBarRepository.deleteByInstrumentId(instrument.getId());
        priceBarRepository.saveAll(entities);

        return new PriceRefreshItemResult(
                normalizedSymbol,
                "SUCCESS",
                entities.size(),
                "Price history refreshed successfully"
        );
    }

    public PriceRefreshAllResult refreshAll() {

        List<Instrument> instruments = instrumentRepository.findAll()
                .stream()
                .filter(instrument ->
                        Boolean.TRUE.equals(instrument.getActive())
                )
                .toList();

        List<PriceRefreshItemResult> results = new ArrayList<>();

        for (Instrument instrument : instruments) {

            String symbol = instrument.getSymbol();

            try {
                results.add(ingestHistory(symbol));

            } catch (HttpClientErrorException ex) {
                results.add(new PriceRefreshItemResult(
                        symbol,
                        "FAILED",
                        0,
                        extractProviderMessage(ex)
                ));

            } catch (RestClientException ex) {
                results.add(new PriceRefreshItemResult(
                        symbol,
                        "FAILED",
                        0,
                        "Market-data provider request failed"
                ));

            } catch (RuntimeException ex) {
                results.add(new PriceRefreshItemResult(
                        symbol,
                        "FAILED",
                        0,
                        ex.getMessage() == null
                                ? "Unexpected refresh failure"
                                : ex.getMessage()
                ));
            }
        }

        int succeeded = (int) results.stream()
                .filter(result ->
                        "SUCCESS".equals(result.status())
                )
                .count();

        int failed = results.size() - succeeded;

        return new PriceRefreshAllResult(
                results.size(),
                succeeded,
                failed,
                results
        );
    }

    private PriceBar toEntity(
            Instrument instrument,
            TwelveDataPriceBar bar
    ) {
        return new PriceBar(
                UUID.randomUUID(),
                instrument,
                LocalDate.parse(bar.datetime()),
                parseDecimal(bar.open()),
                parseDecimal(bar.high()),
                parseDecimal(bar.low()),
                parseDecimal(bar.close()),
                parseLong(bar.volume()),
                LocalDateTime.now()
        );
    }

    private BigDecimal parseDecimal(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value);
    }

    private Long parseLong(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return Long.valueOf(value);
    }

    private String extractProviderMessage(
            HttpClientErrorException ex
    ) {
        String responseBody = ex.getResponseBodyAsString();

        if (responseBody == null || responseBody.isBlank()) {
            return "Provider returned HTTP "
                    + ex.getStatusCode().value();
        }

        return responseBody;
    }
}