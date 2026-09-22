package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.ObservationIngestionResult;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialObservation;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.provider.FinancialPeriodData;
import com.abhiram.atlas.provider.FundamentalsData;
import com.abhiram.atlas.provider.IndianApiProvider;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialObservationRepository;
import com.abhiram.atlas.repository.InstrumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Records provider figures as dated observations.
 *
 * Contrast with FundamentalsIngestionService, which replaces rows and
 * destroys history. This service appends, so the record of what Atlas
 * knew and when survives every subsequent ingest.
 *
 * Restatement handling: when a provider reports a figure that differs
 * from what Atlas already knew, a NEW observation is created and the
 * previous one is superseded. The old row keeps its original
 * knowledge date, so a backtest run at an earlier date still sees the
 * figures that were actually available then.
 *
 * Unchanged figures are not re-recorded. Writing an identical
 * observation every day would bloat the table without adding
 * information.
 */
@Service
public class ObservationIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ObservationIngestionService.class);

    private static final String DEFAULT_UNIT = "CRORE";
    private static final String DEFAULT_CURRENCY = "INR";

    private final IndianApiProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final CompanyRepository companyRepository;
    private final FinancialObservationRepository observationRepository;

    public ObservationIngestionService(
            IndianApiProvider provider,
            InstrumentRepository instrumentRepository,
            CompanyRepository companyRepository,
            FinancialObservationRepository observationRepository
    ) {
        this.provider = provider;
        this.instrumentRepository = instrumentRepository;
        this.companyRepository = companyRepository;
        this.observationRepository = observationRepository;
    }

    @Transactional
    public ObservationIngestionResult ingest(String symbol) {
        return ingest(symbol, LocalDate.now());
    }

    /**
     * Records observations with an explicit knowledge date.
     *
     * The date parameter exists for controlled reconstruction of
     * history from archived data. It is deliberately not exposed on
     * the public endpoint, because backdating an observation to
     * before it was actually knowable would reintroduce exactly the
     * lookahead bias this design prevents.
     */
    @Transactional
    public ObservationIngestionResult ingest(
            String symbol,
            LocalDate knowledgeDate
    ) {
        String normalized = symbol.trim().toUpperCase();

        if (knowledgeDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Knowledge date cannot be in the future"
            );
        }

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

        int newPeriods = 0;
        int restatements = 0;
        int unchanged = 0;

        List<String> restatementDetails = new ArrayList<>();

        for (FinancialPeriodData period : data.periods()) {

            if (!period.isUsable()) {
                continue;
            }

            Optional<FinancialObservation> existing =
                    observationRepository
                            .findFirstByCompanyIdAndFiscalYearAndSupersededAtIsNullOrderByKnowledgeDateDesc(
                                    company.getId(),
                                    period.fiscalYear()
                            );

            if (existing.isEmpty()) {

                record(
                        company,
                        period,
                        knowledgeDate,
                        data.providerName(),
                        1
                );

                newPeriods++;
                continue;
            }

            FinancialObservation previous = existing.get();

            if (!hasChanged(previous, period)) {
                unchanged++;
                continue;
            }

            String detail = describeRestatement(previous, period);

            FinancialObservation replacement = record(
                    company,
                    period,
                    knowledgeDate,
                    data.providerName(),
                    previous.getRevisionNumber() + 1
            );

            previous.supersede(replacement.getId());
            observationRepository.save(previous);

            restatements++;
            restatementDetails.add(detail);

            log.info(
                    "Restatement for {} FY{}: {}",
                    normalized,
                    period.fiscalYear(),
                    detail
            );
        }

        int recorded = newPeriods + restatements;

        return new ObservationIngestionResult(
                normalized,
                recorded > 0 ? "SUCCESS" : "NO_CHANGE",
                recorded,
                newPeriods,
                restatements,
                unchanged,
                knowledgeDate,
                data.providerName(),
                restatementDetails,
                buildMessage(newPeriods, restatements, unchanged)
        );
    }

    private FinancialObservation record(
            Company company,
            FinancialPeriodData period,
            LocalDate knowledgeDate,
            String providerName,
            int revisionNumber
    ) {
        FinancialObservation observation =
                FinancialObservation.record(
                        UUID.randomUUID(),
                        company,
                        period.fiscalYear(),
                        period.fiscalQuarter(),
                        estimatePeriodEnd(period.fiscalYear()),
                        knowledgeDate,
                        period.revenue(),
                        period.netIncome(),
                        period.operatingProfit(),
                        period.operatingCashFlow(),
                        providerName,
                        DEFAULT_UNIT,
                        DEFAULT_CURRENCY,
                        revisionNumber
                );

        return observationRepository.save(observation);
    }

    /**
     * Indian fiscal years end on 31 March. FY2026 therefore ends on
     * 31 March 2026.
     *
     * This is an estimate because the provider does not always supply
     * a reliable period end date. It is used for display and lag
     * calculation, never for point-in-time filtering, which always
     * uses knowledge date.
     */
    private LocalDate estimatePeriodEnd(Integer fiscalYear) {

        if (fiscalYear == null) {
            return null;
        }

        return LocalDate.of(fiscalYear, 3, 31);
    }

    /**
     * Detects a genuine restatement.
     *
     * Comparison uses compareTo rather than equals so that 100.0 and
     * 100.00 are treated as the same figure. Using equals would
     * record a false restatement on every scale change.
     */
    private boolean hasChanged(
            FinancialObservation previous,
            FinancialPeriodData incoming
    ) {
        return differs(previous.getRevenue(), incoming.revenue())
                || differs(
                        previous.getNetIncome(),
                        incoming.netIncome())
                || differs(
                        previous.getOperatingProfit(),
                        incoming.operatingProfit())
                || differs(
                        previous.getOperatingCashFlow(),
                        incoming.operatingCashFlow());
    }

    private boolean differs(
            BigDecimal previous,
            BigDecimal incoming
    ) {
        if (previous == null && incoming == null) {
            return false;
        }

        if (previous == null || incoming == null) {
            return true;
        }

        return previous.compareTo(incoming) != 0;
    }

    private String describeRestatement(
            FinancialObservation previous,
            FinancialPeriodData incoming
    ) {
        List<String> changes = new ArrayList<>();

        addChange(
                changes, "revenue",
                previous.getRevenue(), incoming.revenue());

        addChange(
                changes, "net income",
                previous.getNetIncome(), incoming.netIncome());

        addChange(
                changes, "operating profit",
                previous.getOperatingProfit(),
                incoming.operatingProfit());

        addChange(
                changes, "operating cash flow",
                previous.getOperatingCashFlow(),
                incoming.operatingCashFlow());

        return "FY" + incoming.fiscalYear() + ": "
                + String.join(", ", changes);
    }

    private void addChange(
            List<String> changes,
            String label,
            BigDecimal previous,
            BigDecimal incoming
    ) {
        if (!differs(previous, incoming)) {
            return;
        }

        changes.add(
                label + " " + describe(previous)
                        + " to " + describe(incoming)
        );
    }

    private String describe(BigDecimal value) {
        return value == null
                ? "unreported"
                : value.toPlainString();
    }

    private String buildMessage(
            int newPeriods,
            int restatements,
            int unchanged
    ) {
        if (newPeriods == 0 && restatements == 0) {
            return "No changes. " + unchanged
                    + " period(s) already recorded with identical "
                    + "figures.";
        }

        StringBuilder message = new StringBuilder();

        message.append("Recorded ")
                .append(newPeriods)
                .append(" new period(s)");

        if (restatements > 0) {
            message.append(" and ")
                    .append(restatements)
                    .append(" restatement(s). Historical scores "
                            + "computed before this ingest used "
                            + "different figures.");
        } else {
            message.append(".");
        }

        return message.toString();
    }
}
