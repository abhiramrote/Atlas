package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ScoringProfile;
import com.abhiram.atlas.dto.ScoreabilityResponse;
import com.abhiram.atlas.dto.SectorBaseline;
import com.abhiram.atlas.dto.SectorComparison;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SectorService {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;
    private final SectorNormalizationService normalizationService;

    public SectorService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository,
            SectorNormalizationService normalizationService
    ) {
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
        this.normalizationService = normalizationService;
    }

    public List<SectorBaseline> getBaselines() {

        return normalizationService.computeBaselines()
                .values()
                .stream()
                .sorted((a, b) ->
                        a.sector().compareTo(b.sector()))
                .toList();
    }

    /**
     * Reports whether a company can be meaningfully scored.
     *
     * Returning an explicit refusal with a reason is more useful than
     * a low score, because a number invites action while a stated
     * limitation does not.
     */
    public ScoreabilityResponse getScoreability(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        ScoringProfile profile = ScoringProfile.parse(
                company.getScoringProfile()
        );

        return new ScoreabilityResponse(
                company.getId(),
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                profile.name(),
                profile.isScoreable(),
                describeProfile(profile)
        );
    }

    public SectorComparison compareToSector(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        ScoringProfile profile = ScoringProfile.parse(
                company.getScoringProfile()
        );

        if (!profile.isScoreable()) {
            throw new IllegalStateException(
                    describeProfile(profile)
            );
        }

        String sector = company.getSector();

        List<FinancialStatement> statements =
                financialRepository
                        .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                companyId
                        );

        if (statements.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No financial statements for company: "
                            + companyId
            );
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

        Map<String, SectorBaseline> baselines =
                normalizationService.computeBaselines();

        SectorBaseline baseline = baselines.get(sector);

        if (baseline == null) {
            return new SectorComparison(
                    sector,
                    0,
                    false,
                    operatingMargin,
                    null,
                    null,
                    profitMargin,
                    null,
                    null,
                    "No sector baseline is available."
            );
        }

        boolean sufficient =
                Boolean.TRUE.equals(baseline.sufficient());

        return new SectorComparison(
                sector,
                baseline.peerCount(),
                sufficient,
                operatingMargin,
                baseline.medianOperatingMargin(),
                sufficient
                        ? normalizationService.relativeToSector(
                                operatingMargin,
                                baseline.medianOperatingMargin())
                        : null,
                profitMargin,
                baseline.medianProfitMargin(),
                sufficient
                        ? normalizationService.relativeToSector(
                                profitMargin,
                                baseline.medianProfitMargin())
                        : null,
                baseline.note()
        );
    }

    private String describeProfile(ScoringProfile profile) {

        return switch (profile) {

            case OPERATING_COMPANY ->
                    "Standard metrics apply to this company.";

            case BANK ->
                    "Atlas cannot yet score banks. Interest income "
                            + "is not revenue, and operating cash "
                            + "flow reflects deposit and lending "
                            + "flows rather than profitability. "
                            + "Bank scoring requires net interest "
                            + "margin, cost to income and asset "
                            + "quality metrics that Atlas does not "
                            + "ingest.";

            case FINANCIAL_SERVICES ->
                    "Atlas cannot yet score non-bank financial "
                            + "companies. Their statements follow "
                            + "financial sector conventions that "
                            + "the current metrics do not model.";

            case UNKNOWN ->
                    "This company has no scoring profile, so Atlas "
                            + "cannot determine whether its metrics "
                            + "are interpretable.";
        };
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