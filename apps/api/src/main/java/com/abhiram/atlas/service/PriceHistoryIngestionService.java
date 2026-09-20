package com.abhiram.atlas.service;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PriceHistoryIngestionService {

    private final TwelveDataProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;

    public PriceHistoryIngestionService(
            TwelveDataProvider provider,
            InstrumentRepository instrumentRepository,
            PriceBarRepository priceBarRepository) {

        this.provider = provider;
        this.instrumentRepository = instrumentRepository;
        this.priceBarRepository = priceBarRepository;
    }

    @Transactional
public void ingestHistory(String symbol) {

    Instrument instrument =
            instrumentRepository
                    .findBySymbolIgnoreCase(symbol)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Instrument not found: " + symbol
                            ));

    TwelveDataTimeSeriesResponse response =
            provider.fetchPriceHistory(symbol);

    priceBarRepository.deleteByInstrumentId(
            instrument.getId()
    );

    response.values()
            .forEach(bar -> saveBar(
                    instrument,
                    bar
            ));
}

private void saveBar(
        Instrument instrument,
        TwelveDataPriceBar bar) {

    PriceBar entity =
            new PriceBar(
                    UUID.randomUUID(),
                    instrument,
                    LocalDate.parse(bar.datetime()),
                    new BigDecimal(bar.open()),
                    new BigDecimal(bar.high()),
                    new BigDecimal(bar.low()),
                    new BigDecimal(bar.close()),
                    Long.valueOf(bar.volume()),
                    LocalDateTime.now()
            );

    priceBarRepository.save(entity);
}
}