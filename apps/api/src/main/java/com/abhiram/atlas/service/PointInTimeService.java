package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.PointInTimeSnapshot;
import com.abhiram.atlas.dto.PointInTimePeriod;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialObservation;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialObservationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reconstructs what Atlas knew about a company on a past date.
 *
 * This is the foundation for backtesting. Without it, any historical
 * evaluation silently uses today's restated figures and produces
 * results that cannot be reproduced live.
 *
 * Data quality is reported explicitly rather than assumed. A snapshot
 * built entirely from backfilled rows carries an approximated
 * knowledge date and should not be trusted for evaluation, so the
 * response says so.
 */
@Service
@Transactional(readOnly = true)
public class PointInTimeService {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final String BACKFILL_PROVIDER = "BACKFILL";

    private final CompanyRepository companyRepository;
    private final FinancialObservationRepository observationRepository;

    public PointInTimeService(
            CompanyRepository companyRepository,
            FinancialObservationRepository observationRepository
    ) {
        this.companyRepository = companyRepository;
        this.observationRepository = observationRepository;
    }

    /**
     * Returns the financial view as it stood on the given date.
     *
     * A future date is rejected. Requesting tomorrow's view would
     * return today's data under a misleading label and is almost
     * always a caller mistake.
     */
    public PointInTimeSnapshot getSnapshot(
            UUID companyId,
            LocalDate asOfDate
    ) {
        if (asOfDate == null) {
            throw new IllegalArgumentException(
                    "As-of date is required"
            );
        }

        if (asOfDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "As-of date cannot be in the future"
            );
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        List<FinancialObservation> observations =
                observationRepository.findAsOf(companyId, asOfDate);

        LocalDate earliest = observationRepository
                .findEarliestKnowledgeDate(companyId)
                .orElse(null);

        List<PointInTimePeriod> periods = observations.stream()
                .map(this::toPeriod)
                .toList();

        return new PointInTimeSnapshot(
                companyId,
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                asOfDate,
                periods.size(),
                earliest,
                buildDataQualityNote(observations, asOfDate, earliest),
                periods
        );
    }

    private PointInTimePeriod toPeriod(
            FinancialObservation observation
    ) {
        return new PointInTimePeriod(
                observation.getFiscalYear(),
                observation.getFiscalQuarter(),
                observation.getPeriodEndDate(),
                observation.getKnowledgeDate(),
                observation.getRevenue(),
                observation.getNetIncome(),
                observation.getOperatingProfit(),
                observation.getOperatingCashFlow(),
                percentage(
                        observation.getOperatingProfit(),
                        observation.getRevenue()
                ),
                percentage(
                        observation.getNetIncome(),
                        observation.getRevenue()
                ),
                observation.getRevisionNumber(),
                observation.getSourceProvider(),
                observation.getUnit()
        );
    }

    /**
     * Describes how far the snapshot can be trusted.
     *
     * Stating the limitation is more useful than returning a clean
     * looking result that quietly rests on approximated dates.
     */
    private String buildDataQualityNote(
            List<FinancialObservation> observations,
            LocalDate asOfDate,
            LocalDate earliest
    ) {
        if (observations.isEmpty()) {

            if (earliest == null) {
                return "No financial observations exist for this "
                        + "company.";
            }

            return "No data was known on " + asOfDate
                    + ". The earliest observation dates from "
                    + earliest + ".";
        }

        long backfilled = observations.stream()
                .filter(o -> BACKFILL_PROVIDER
                        .equals(o.getSourceProvider()))
                .count();

        if (backfilled == observations.size()) {
            return "All observations were backfilled with an "
                    + "approximated knowledge date. This snapshot "
                    + "is not reliable for point-in-time evaluation.";
        }

        if (backfilled > 0) {
            return backfilled + " of " + observations.size()
                    + " observations were backfilled with an "
                    + "approximated knowledge date.";
        }

        return "All observations carry a recorded knowledge date.";
    }

    private BigDecimal percentage(
            BigDecimal value,
            BigDecimal base
    ) {
        if (value == null || base == null) {
            return null;
        }

        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return value
                .divide(base, 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
