package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ScoringProfile;
import com.abhiram.atlas.dto.SectorBaseline;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes sector baselines so a company can be judged against its
 * own industry rather than against every other business.
 *
 * WHY THIS IS NEEDED
 *
 * Software structurally earns higher operating margins than cement.
 * Scoring both against the same absolute threshold rewards the
 * industry rather than the company. The useful question is whether a
 * company is strong for its sector.
 *
 * WHY THE MEDIAN, NOT THE MEAN
 *
 * Sector groups here are small, often two or three companies. A
 * single outlier would drag a mean badly. The median is stable under
 * exactly that condition.
 *
 * IMPORTANT LIMITATION
 *
 * With twenty companies most sectors contain one or two members. A
 * baseline computed from a single company is that company, which
 * makes normalisation meaningless. The service reports the peer count
 * so callers can see when a baseline is too thin to trust, and
 * declines to normalise below a minimum.
 */
@Service
public class SectorNormalizationService {

    /**
     * Minimum peers required before a sector baseline is meaningful.
     *
     * Three allows a genuine median. Two would make the median the
     * average of the only two members, which tells you nothing about
     * relative standing.
     */
    private static final int MINIMUM_PEERS = 3;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public SectorNormalizationService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository
    ) {
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    /**
     * Builds baselines for every sector with enough scoreable peers.
     *
     * Unscoreable companies are excluded from the peer set. Including
     * a bank's distorted margin in a sector median would corrupt the
     * baseline for every company in that sector.
     */
    public Map<String, SectorBaseline> computeBaselines() {

        Map<String, List<BigDecimal>> operatingMargins =
                new HashMap<>();

        Map<String, List<BigDecimal>> profitMargins =
                new HashMap<>();

        Map<String, Integer> peerCounts = new HashMap<>();

        for (Company company : companyRepository.findAll()) {

            ScoringProfile profile = ScoringProfile.parse(
                    company.getScoringProfile()
            );

            if (!profile.isScoreable()) {
                continue;
            }

            String sector = company.getSector();

            if (sector == null || sector.isBlank()) {
                continue;
            }

            List<FinancialStatement> statements =
                    financialRepository
                            .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                    company.getId()
                            );

            if (statements.isEmpty()) {
                continue;
            }

            FinancialStatement latest = statements.get(0);

            BigDecimal operatingMargin = percentage(
                    latest.getOperatingProfit(),
                    latest.getRevenue()
            );

            BigDecimal profitMargin = percentage(
                    latest.getNetIncome(),
                    latest.getRevenue()
            );

            if (operatingMargin == null && profitMargin == null) {
                continue;
            }

            peerCounts.merge(sector, 1, Integer::sum);

            if (operatingMargin != null) {
                operatingMargins
                        .computeIfAbsent(
                                sector,
                                key -> new ArrayList<>())
                        .add(operatingMargin);
            }

            if (profitMargin != null) {
                profitMargins
                        .computeIfAbsent(
                                sector,
                                key -> new ArrayList<>())
                        .add(profitMargin);
            }
        }

        Map<String, SectorBaseline> baselines = new HashMap<>();

        for (Map.Entry<String, Integer> entry
                : peerCounts.entrySet()) {

            String sector = entry.getKey();
            int peers = entry.getValue();

            boolean sufficient = peers >= MINIMUM_PEERS;

            baselines.put(sector, new SectorBaseline(
                    sector,
                    peers,
                    sufficient,
                    median(operatingMargins.get(sector)),
                    median(profitMargins.get(sector)),
                    sufficient
                            ? null
                            : "Only " + peers + " scoreable peer"
                                    + (peers == 1 ? "" : "s")
                                    + " in this sector. A baseline "
                                    + "needs at least "
                                    + MINIMUM_PEERS
                                    + " to be meaningful, so "
                                    + "normalisation is not applied."
            ));
        }

        return baselines;
    }

    /**
     * Expresses a value relative to its sector median.
     *
     * Returns null when the baseline is too thin or the median is
     * zero. Returning a default would hide the fact that the
     * comparison could not be made.
     */
    public BigDecimal relativeToSector(
            BigDecimal value,
            BigDecimal sectorMedian
    ) {
        if (value == null || sectorMedian == null) {
            return null;
        }

        if (sectorMedian.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return value
                .subtract(sectorMedian)
                .divide(
                        sectorMedian.abs(),
                        6,
                        RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> values) {

        if (values == null || values.isEmpty()) {
            return null;
        }

        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int size = sorted.size();
        int middle = size / 2;

        if (size % 2 == 1) {
            return sorted.get(middle);
        }

        return sorted.get(middle - 1)
                .add(sorted.get(middle))
                .divide(
                        BigDecimal.valueOf(2),
                        2,
                        RoundingMode.HALF_UP);
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
