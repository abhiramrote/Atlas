package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.IngestionResult;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.provider.CompanyProfileData;
import com.abhiram.atlas.provider.MarketDataProvider;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.InstrumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MarketDataIngestionService {

    private final MarketDataProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final CompanyRepository companyRepository;

    public MarketDataIngestionService(
            MarketDataProvider provider,
            InstrumentRepository instrumentRepository,
            CompanyRepository companyRepository
    ) {
        this.provider = provider;
        this.instrumentRepository = instrumentRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public IngestionResult ingestCompany(String symbol) {

        String normalizedSymbol = normalizeSymbol(symbol);

        Instrument instrument = instrumentRepository
                .findBySymbolIgnoreCase(normalizedSymbol)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instrument not found: "
                                        + normalizedSymbol
                        )
                );

        Company company = companyRepository
                .findByInstrumentId(instrument.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found for instrument: "
                                        + normalizedSymbol
                        )
                );

        CompanyProfileData providerData =
                provider.fetchCompanyData(normalizedSymbol);

        company.updateProfile(
                providerData.sector(),
                providerData.industry(),
                providerData.website(),
                providerData.description()
        );

        companyRepository.save(company);

        return new IngestionResult(
                normalizedSymbol,
                provider.getProviderName(),
                "SUCCESS",
                "Company profile updated successfully",
                LocalDateTime.now()
        );
    }

    private String normalizeSymbol(String symbol) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol must not be empty"
            );
        }

        return symbol.trim().toUpperCase();
    }
}