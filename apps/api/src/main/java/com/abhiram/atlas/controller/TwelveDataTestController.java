package com.abhiram.atlas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.repository.InstrumentRepository;
import com.abhiram.atlas.provider.CompanyProfileData;
import com.abhiram.atlas.provider.TwelveDataProvider;
import com.abhiram.atlas.provider.TwelveDataSearchResponse;
import com.abhiram.atlas.provider.TwelveDataTimeSeriesResponse;
import com.abhiram.atlas.service.PriceHistoryIngestionService;

@RestController
@RequestMapping("/api/test")
public class TwelveDataTestController {

    private final TwelveDataProvider provider;
    private final PriceHistoryIngestionService service;
    private final InstrumentRepository instrumentRepository;
    public TwelveDataTestController(
        TwelveDataProvider provider,
        PriceHistoryIngestionService service,
        InstrumentRepository instrumentRepository) {

    this.provider = provider;
    this.service = service;
    this.instrumentRepository = instrumentRepository;
}



    @GetMapping("/{symbol}")
    public TwelveDataSearchResponse test(
            @PathVariable String symbol) {

        return provider.fetchProfile(symbol);
    }
    @GetMapping("/profile/{symbol}")
    public CompanyProfileData profile(
        @PathVariable String symbol) {

    return provider.getCompanyProfile(symbol);
    }
    @GetMapping("/price/{symbol}")
public String getPrice(
        @PathVariable String symbol) {

    return provider.fetchPrice(symbol);
}
@GetMapping("/history/{symbol}")
public TwelveDataTimeSeriesResponse history(
        @PathVariable String symbol) {

    return provider.fetchPriceHistory(symbol);
}
@PostMapping("/history/ingest/{symbol}")
public String ingestHistory(
        @PathVariable String symbol) {

    service.ingestHistory(symbol);

    return "History imported successfully";
}
@GetMapping("/instrument/{symbol}")
public Instrument getInstrument(
        @PathVariable String symbol) {

    return instrumentRepository
            .findBySymbolIgnoreCase(symbol)
            .orElseThrow();
}
}
