package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.FundamentalsIngestionResult;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.provider.FinancialPeriodData;
import com.abhiram.atlas.provider.FundamentalsData;
import com.abhiram.atlas.provider.IndianApiProvider;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;
import com.abhiram.atlas.repository.InstrumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ingests real company fundamentals and replaces development
 * fixtures.
 *
 * Replacement strategy: existing statements for a company are deleted
 * before the provider's periods are written. A merge would leave a
 * mixture of synthetic and real rows with no way to tell them apart,
 * and growth metrics computed across that mixture would be
 * meaningless.
 *
 * Ingestion is refused when the provider returns fewer than two
 * usable periods, because Atlas scoring requires two periods to
 * compute growth. Deleting good fixtures and writing insufficient
 * real data would leave the company unscoreable.
 */
@Service
public class FundamentalsIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    FundamentalsIngestionService.class);

    private static final int MINIMUM_PERIODS = 2;

    private final IndianApiProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public FundamentalsIngestionService(
            IndianApiProvider provider,
            InstrumentRepository instrumentRepository,
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository
    ) {
        this.provider = provider;
        this.instrumentRepository = instrumentRepository;
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    @Transactional
    public FundamentalsIngestionResult ingest(String symbol) {

        String normalized = symbol.trim().toUpperCase();

        Instrument instrument = instrumentRepository
                .findBySymbolIgnoreCase(normalized)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instrument not found: " + normalized
                        )
                );

        Company company = companyRepository
                .findByInstrumentId(instrument.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found for instrument: "
                                        + normalized
                        )
                );

        FundamentalsData data =
                provider.fetchFundamentals(normalized);

        List<FinancialPeriodData> periods = data.periods();

        if (periods.size() < MINIMUM_PERIODS) {
            return new FundamentalsIngestionResult(
                    normalized,
                    "SKIPPED",
                    0,
                    0,
                    provider.getProviderName(),
                    "Provider returned only " + periods.size()
                            + " usable period(s). Atlas requires "
                            + MINIMUM_PERIODS
                            + " to compute growth, so existing data "
                            + "was left unchanged."
            );
        }

        int replaced = financialRepository
                .findByCompanyId(company.getId())
                .size();

        financialRepository.deleteAll(
        financialRepository.findByCompanyId(company.getId())
        );

        financialRepository.flush();


        int imported = 0;

        for (FinancialPeriodData period : periods) {

            FinancialStatement statement = buildStatement(
                    company,
                    period
            );

            financialRepository.save(statement);
            imported++;
        }

        updateCompanyProfile(company, data);

        log.info(
                "Imported {} financial periods for {} from {}",
                imported,
                normalized,
                provider.getProviderName()
        );

        return new FundamentalsIngestionResult(
                normalized,
                "SUCCESS",
                imported,
                replaced,
                provider.getProviderName(),
                "Replaced " + replaced + " existing period(s) with "
                        + imported + " provider period(s)"
        );
    }

    /**
     * Updates the company profile only where the provider supplied a
     * value. A blank field from the provider must not erase a
     * description that is already present.
     */
    private void updateCompanyProfile(
            Company company,
            FundamentalsData data
    ) {
        String sector = preferProvided(
                data.sector(),
                company.getSector()
        );

        String industry = preferProvided(
                data.industry(),
                company.getIndustry()
        );

        String description = preferProvided(
                data.description(),
                company.getDescription()
        );

        company.updateProfile(
                sector,
                industry,
                company.getWebsite(),
                description
        );

        companyRepository.save(company);
    }

    private String preferProvided(
            String provided,
            String existing
    ) {
        return Optional.ofNullable(provided)
                .filter(value -> !value.isBlank())
                .orElse(existing);
    }

    /**
     * Builds a statement using reflection-free construction.
     *
     * FinancialStatement has no public constructor in Atlas, so this
     * method depends on the factory added in APPLY.md.
     */
    private FinancialStatement buildStatement(
            Company company,
            FinancialPeriodData period
    ) {
        return FinancialStatement.fromProvider(
                UUID.randomUUID(),
                company,
                period.fiscalYear(),
                period.fiscalQuarter(),
                period.revenue(),
                period.netIncome(),
                period.operatingProfit(),
                period.operatingCashFlow(),
                LocalDateTime.now()
        );
    }
}
