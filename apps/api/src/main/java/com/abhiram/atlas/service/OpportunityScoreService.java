package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.OpportunityScoreResponse;
import com.abhiram.atlas.dto.ScoreFactorResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class OpportunityScoreService {

    private static final String POLICY_VERSION = "v1.0";
    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public OpportunityScoreService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository
    ) {
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    public OpportunityScoreResponse scoreCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        List<FinancialStatement> statements =
                financialRepository
                        .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                companyId
                        );

        if (statements.size() < 2) {
            throw new ResourceNotFoundException(
                    "At least two financial periods are required "
                            + "to score company: " + companyId
            );
        }

        FinancialStatement current = statements.get(0);
        FinancialStatement previous = statements.get(1);

        BigDecimal revenueGrowth = calculateGrowth(
                current.getRevenue(),
                previous.getRevenue()
        );

        BigDecimal netIncomeGrowth = calculateGrowth(
                current.getNetIncome(),
                previous.getNetIncome()
        );

        BigDecimal operatingProfitGrowth = calculateGrowth(
                current.getOperatingProfit(),
                previous.getOperatingProfit()
        );

        BigDecimal operatingCashFlowGrowth = calculateGrowth(
                current.getOperatingCashFlow(),
                previous.getOperatingCashFlow()
        );

        BigDecimal profitMargin = calculateMargin(
                current.getNetIncome(),
                current.getRevenue()
        );

        BigDecimal operatingMargin = calculateMargin(
                current.getOperatingProfit(),
                current.getRevenue()
        );

        List<ScoreFactorResponse> factors = new ArrayList<>();

        factors.add(scoreGrowthFactor(
                "Revenue Growth",
                revenueGrowth,
                20
        ));

        factors.add(scoreGrowthFactor(
                "Net Income Growth",
                netIncomeGrowth,
                20
        ));

        factors.add(scoreGrowthFactor(
                "Operating Profit Growth",
                operatingProfitGrowth,
                15
        ));

        factors.add(scoreGrowthFactor(
                "Operating Cash Flow Growth",
                operatingCashFlowGrowth,
                15
        ));

        factors.add(scoreMarginFactor(
                "Profit Margin",
                profitMargin,
                15
        ));

        factors.add(scoreMarginFactor(
                "Operating Margin",
                operatingMargin,
                15
        ));

        int score = factors.stream()
                .mapToInt(ScoreFactorResponse::pointsAwarded)
                .sum();

        return new OpportunityScoreResponse(
                company.getId(),
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                score,
                100,
                determineRating(score),
                POLICY_VERSION,
                current.getFiscalYear(),
                previous.getFiscalYear(),
                factors
        );
    }

    private ScoreFactorResponse scoreGrowthFactor(
            String name,
            BigDecimal value,
            int maximumPoints
    ) {
        if (value == null) {
            return new ScoreFactorResponse(
                    name,
                    null,
                    0,
                    maximumPoints,
                    "Points not awarded because data is unavailable"
            );
        }

        int points;
        String explanation;

        if (value.compareTo(BigDecimal.valueOf(15)) >= 0) {
            points = maximumPoints;
            explanation = name + " is at least 15%";
        } else if (value.compareTo(BigDecimal.valueOf(10)) >= 0) {
            points = roundPoints(maximumPoints, 0.75);
            explanation = name + " is between 10% and 15%";
        } else if (value.compareTo(BigDecimal.valueOf(5)) >= 0) {
            points = roundPoints(maximumPoints, 0.50);
            explanation = name + " is between 5% and 10%";
        } else if (value.compareTo(BigDecimal.ZERO) > 0) {
            points = roundPoints(maximumPoints, 0.25);
            explanation = name + " is positive but below 5%";
        } else {
            points = 0;
            explanation = name + " is zero or negative";
        }

        return new ScoreFactorResponse(
                name,
                value,
                points,
                maximumPoints,
                explanation
        );
    }

    private ScoreFactorResponse scoreMarginFactor(
            String name,
            BigDecimal value,
            int maximumPoints
    ) {
        if (value == null) {
            return new ScoreFactorResponse(
                    name,
                    null,
                    0,
                    maximumPoints,
                    "Points not awarded because data is unavailable"
            );
        }

        int points;
        String explanation;

        if (value.compareTo(BigDecimal.valueOf(20)) >= 0) {
            points = maximumPoints;
            explanation = name + " is at least 20%";
        } else if (value.compareTo(BigDecimal.valueOf(15)) >= 0) {
            points = roundPoints(maximumPoints, 0.75);
            explanation = name + " is between 15% and 20%";
        } else if (value.compareTo(BigDecimal.valueOf(10)) >= 0) {
            points = roundPoints(maximumPoints, 0.50);
            explanation = name + " is between 10% and 15%";
        } else if (value.compareTo(BigDecimal.ZERO) > 0) {
            points = roundPoints(maximumPoints, 0.25);
            explanation = name + " is positive but below 10%";
        } else {
            points = 0;
            explanation = name + " is zero or negative";
        }

        return new ScoreFactorResponse(
                name,
                value,
                points,
                maximumPoints,
                explanation
        );
    }

    private BigDecimal calculateGrowth(
            BigDecimal currentValue,
            BigDecimal previousValue
    ) {
        if (currentValue == null || previousValue == null) {
            return null;
        }

        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return currentValue
                .subtract(previousValue)
                .divide(previousValue, 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMargin(
            BigDecimal value,
            BigDecimal revenue
    ) {
        if (value == null || revenue == null) {
            return null;
        }

        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return value
                .divide(revenue, 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int roundPoints(
            int maximumPoints,
            double percentage
    ) {
        return (int) Math.round(maximumPoints * percentage);
    }

    private String determineRating(int score) {

        if (score >= 85) {
            return "STRONG";
        }

        if (score >= 70) {
            return "GOOD";
        }

        if (score >= 50) {
            return "MODERATE";
        }

        return "WEAK";
    }
    public List<OpportunityScoreResponse> getRankedOpportunities(
        int limit
) {
    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }

    return companyRepository.findAll()
            .stream()
            .map(company -> {
                try {
                    return scoreCompany(company.getId());
                } catch (RuntimeException ex) {
                    // A company without sufficient financial
                    // history cannot be ranked. Excluding it is
                    // correct; failing the whole ranking is not.
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .sorted(
                    Comparator.comparingInt(
                            OpportunityScoreResponse::score
                    )
                    .reversed()
                    .thenComparing(
                            OpportunityScoreResponse::symbol
                    )
            )
            .limit(limit)
            .toList();
}

    public Integer getScore(UUID companyId) {
    return scoreCompany(companyId).score();
}
}