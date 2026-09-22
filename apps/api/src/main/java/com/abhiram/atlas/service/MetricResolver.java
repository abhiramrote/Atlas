package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.FinancialGrowthResponse;
import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.dto.RiskMetricsResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves metric names used in invalidation conditions into
 * current observed values.
 *
 * Central design rule: a metric that cannot be computed resolves to
 * null, never to zero or a default. A condition such as
 * "operating margin BELOW 18" would fire falsely on a zero default
 * every time a provider call failed, which would destroy trust in the
 * entire monitoring system. Unknown must stay unknown.
 *
 * All metrics are resolved in a single pass per company so that one
 * monitoring run does not repeat the same computation for every
 * condition attached to a thesis.
 */
@Service
@Transactional(readOnly = true)
public class MetricResolver {

    private static final Logger log =
            LoggerFactory.getLogger(MetricResolver.class);

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    /**
     * Metric names that Atlas can currently evaluate. Conditions
     * referring to anything outside this set are reported as
     * unsupported rather than silently ignored.
     */
    public static final Set<String> SUPPORTED_METRICS = Set.of(
            "OPERATING_MARGIN",
            "PROFIT_MARGIN",
            "REVENUE_GROWTH",
            "NET_INCOME_GROWTH",
            "OPERATING_PROFIT_GROWTH",
            "OPERATING_CASH_FLOW_GROWTH",
            "MAX_DRAWDOWN",
            "VOLATILITY",
            "FINAL_SCORE",
            "FUNDAMENTAL_SCORE",
            "TECHNICAL_SCORE"
    );

    private final FinancialStatementRepository financialRepository;
    private final FinancialGrowthService growthService;
    private final RiskMetricsService riskService;
    private final OpportunityScoreV2Service scoreService;

    public MetricResolver(
            FinancialStatementRepository financialRepository,
            FinancialGrowthService growthService,
            RiskMetricsService riskService,
            OpportunityScoreV2Service scoreService
    ) {
        this.financialRepository = financialRepository;
        this.growthService = growthService;
        this.riskService = riskService;
        this.scoreService = scoreService;
    }

    /**
     * Computes every supported metric for a company.
     *
     * Each source is queried independently and failures are isolated,
     * so a missing price history does not prevent margin conditions
     * from being evaluated.
     */
    public Map<String, BigDecimal> resolveAll(Company company) {

        Map<String, BigDecimal> metrics = new HashMap<>();

        addMargins(company, metrics);
        addGrowth(company, metrics);
        addRisk(company, metrics);
        addScores(company, metrics);

        return metrics;
    }

    public boolean isSupported(String metric) {
        return metric != null
                && SUPPORTED_METRICS.contains(
                        metric.trim().toUpperCase());
    }

    private void addMargins(
            Company company,
            Map<String, BigDecimal> metrics
    ) {
        try {
            List<FinancialStatement> statements =
                    financialRepository
                            .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                    company.getId());

            if (statements.isEmpty()) {
                return;
            }

            FinancialStatement current = statements.get(0);

            metrics.put(
                    "OPERATING_MARGIN",
                    percentage(
                            current.getOperatingProfit(),
                            current.getRevenue()
                    )
            );

            metrics.put(
                    "PROFIT_MARGIN",
                    percentage(
                            current.getNetIncome(),
                            current.getRevenue()
                    )
            );

        } catch (RuntimeException ex) {
            log.debug(
                    "Margin metrics unavailable for {}: {}",
                    company.getId(),
                    ex.getMessage()
            );
        }
    }

    private void addGrowth(
            Company company,
            Map<String, BigDecimal> metrics
    ) {
        try {
            FinancialGrowthResponse growth =
                    growthService.getGrowth(company.getId());

            metrics.put(
                    "REVENUE_GROWTH",
                    growth.revenueGrowthPercent()
            );

            metrics.put(
                    "NET_INCOME_GROWTH",
                    growth.netIncomeGrowthPercent()
            );

            metrics.put(
                    "OPERATING_PROFIT_GROWTH",
                    growth.operatingProfitGrowthPercent()
            );

            metrics.put(
                    "OPERATING_CASH_FLOW_GROWTH",
                    growth.operatingCashFlowGrowthPercent()
            );

        } catch (RuntimeException ex) {
            log.debug(
                    "Growth metrics unavailable for {}: {}",
                    company.getId(),
                    ex.getMessage()
            );
        }
    }

    private void addRisk(
            Company company,
            Map<String, BigDecimal> metrics
    ) {
        try {
            RiskMetricsResponse risk =
                    riskService.getRiskMetrics(
                            company.getInstrument().getId());

            metrics.put(
                    "MAX_DRAWDOWN",
                    risk.maximumDrawdownPercent()
            );

            metrics.put(
                    "VOLATILITY",
                    risk.annualisedVolatilityPercent()
            );

        } catch (RuntimeException ex) {
            log.debug(
                    "Risk metrics unavailable for {}: {}",
                    company.getId(),
                    ex.getMessage()
            );
        }
    }

    private void addScores(
            Company company,
            Map<String, BigDecimal> metrics
    ) {
        try {
            OpportunityScoreV2Response score =
                    scoreService.score(company.getId());

            metrics.put(
                    "FINAL_SCORE",
                    toDecimal(score.finalScore())
            );

            metrics.put(
                    "FUNDAMENTAL_SCORE",
                    toDecimal(score.fundamentalScore())
            );

            metrics.put(
                    "TECHNICAL_SCORE",
                    toDecimal(score.technicalScore())
            );

        } catch (RuntimeException ex) {
            log.debug(
                    "Score metrics unavailable for {}: {}",
                    company.getId(),
                    ex.getMessage()
            );
        }
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

    private BigDecimal toDecimal(Integer value) {
        return value == null
                ? null
                : BigDecimal.valueOf(value);
    }
}
