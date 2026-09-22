package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.DataQualitySeverity;
import com.abhiram.atlas.dto.DataQualityIssue;
import com.abhiram.atlas.dto.DataQualityReport;
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

/**
 * Detects financial data that is almost certainly wrong.
 *
 * THE CASE THAT PRODUCED THIS
 *
 * HINDALCO reported FY2025 net income of 198,699 crore on revenue of
 * 476,992 crore. That is a 42 percent net margin for an aluminium
 * business whose every other year sits between 3 and 7 percent.
 *
 * Atlas scored the company 8 out of 100 because it compared a normal
 * FY2026 against that corrupted FY2025. The score was arithmetically
 * correct and completely meaningless.
 *
 * Nothing in the pipeline objected. A single bad provider row flowed
 * through growth calculation into scoring into the ranking, and was
 * only caught by manual inspection of an outlier.
 *
 * DESIGN PRINCIPLE
 *
 * These checks never modify or discard data. Silently correcting a
 * figure would hide a provider problem and make the stored history
 * disagree with the source. The checks annotate, and callers decide.
 *
 * Thresholds are deliberately loose. A check that fires on ordinary
 * volatility trains you to ignore it, which is worse than having no
 * check. These are tuned to catch impossibilities, not surprises.
 */
@Service
@Transactional(readOnly = true)
public class DataQualityService {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    /**
     * Net margin above this is implausible for any non-financial
     * company reporting conventionally.
     *
     * Set at 60 rather than something tighter because a few genuine
     * businesses do run very high margins. Regulated transmission
     * and some pharmaceutical licensing models reach forty or fifty.
     * Sixty is high enough that crossing it almost always means the
     * figure is not really net income.
     */
    private static final BigDecimal IMPLAUSIBLE_MARGIN =
            BigDecimal.valueOf(40);

    /**
     * Net margin below this suggests either severe distress or a
     * reporting problem. Flagged as a warning, not an error,
     * because genuine large losses do happen.
     */
    private static final BigDecimal SEVERE_LOSS_MARGIN =
            BigDecimal.valueOf(-50);

    /**
     * Year on year change beyond this is treated as a probable data
     * problem rather than a business event.
     *
     * Three hundred percent allows for genuine cyclical swings,
     * recoveries from a low base and commodity cycles. A tighter
     * bound would fire constantly on cyclical companies.
     */
    private static final BigDecimal EXTREME_CHANGE =
            BigDecimal.valueOf(300);

    /**
     * Revenue change beyond this is suspicious even though revenue
     * is more stable than profit. A business rarely doubles or
     * halves its top line in one year without a merger or a change
     * in reporting basis.
     */
    private static final BigDecimal EXTREME_REVENUE_CHANGE =
            BigDecimal.valueOf(60);

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public DataQualityService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository
    ) {
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    public DataQualityReport check(UUID companyId) {

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

        List<DataQualityIssue> issues = new ArrayList<>();

        for (FinancialStatement statement : statements) {
            issues.addAll(checkMargins(statement));
        }

        // Year on year checks need consecutive pairs.
        for (int index = 0; index < statements.size() - 1; index++) {

            FinancialStatement current = statements.get(index);
            FinancialStatement previous = statements.get(index + 1);

            issues.addAll(checkChanges(current, previous));
        }

        long errorCount = issues.stream()
                .filter(issue -> DataQualitySeverity.ERROR.name()
                        .equals(issue.severity()))
                .count();

        // Scoring uses the two most recent periods. An error in an
        // older period is worth reporting but does not invalidate a
        // current score.
        boolean scoringAffected = statements.size() >= 2
                && issues.stream()
                        .filter(issue ->
                                DataQualitySeverity.ERROR.name()
                                        .equals(issue.severity()))
                        .anyMatch(issue ->
                                affectsScoringPeriods(
                                        issue,
                                        statements));

        return new DataQualityReport(
                companyId,
                company.getInstrument().getSymbol(),
                statements.size(),
                issues.size(),
                (int) errorCount,
                !scoringAffected,
                buildSummary(
                        issues.size(),
                        (int) errorCount,
                        scoringAffected),
                issues
        );
    }

    /**
     * Checks whether an issue touches a period that scoring uses.
     *
     * Scoring compares the two most recent periods, so a corrupted
     * FY2019 does not make today's score unreliable.
     */
    private boolean affectsScoringPeriods(
            DataQualityIssue issue,
            List<FinancialStatement> statements
    ) {
        Integer currentYear =
                statements.get(0).getFiscalYear();

        Integer previousYear =
                statements.get(1).getFiscalYear();

        return issue.fiscalYear().equals(currentYear)
                || issue.fiscalYear().equals(previousYear);
    }

    private List<DataQualityIssue> checkMargins(
            FinancialStatement statement
    ) {
        List<DataQualityIssue> issues = new ArrayList<>();

        BigDecimal netMargin = percentage(
                statement.getNetIncome(),
                statement.getRevenue()
        );

        if (netMargin == null) {
            return issues;
        }

        if (netMargin.compareTo(IMPLAUSIBLE_MARGIN) > 0) {

            issues.add(new DataQualityIssue(
                    "IMPLAUSIBLE_NET_MARGIN",
                    DataQualitySeverity.ERROR.name(),
                    statement.getFiscalYear(),
                    "NET_MARGIN",
                    netMargin,
                    "below " + IMPLAUSIBLE_MARGIN + " percent",
                    "A net margin this high is not achievable by a "
                            + "conventionally reporting operating "
                            + "company. The reported net income is "
                            + "probably not net income, or the "
                            + "period mixes consolidated and "
                            + "standalone figures."
            ));
        }

        if (netMargin.compareTo(SEVERE_LOSS_MARGIN) < 0) {

            issues.add(new DataQualityIssue(
                    "SEVERE_LOSS_MARGIN",
                    DataQualitySeverity.WARNING.name(),
                    statement.getFiscalYear(),
                    "NET_MARGIN",
                    netMargin,
                    "above " + SEVERE_LOSS_MARGIN + " percent",
                    "A loss larger than half of revenue is possible "
                            + "but unusual. Verify against the "
                            + "source before relying on any score "
                            + "derived from this period."
            ));
        }

        // Operating profit exceeding revenue is arithmetically
        // possible only when other income is included in operating
        // profit, which means the figures are not comparable with
        // other companies.
        if (statement.getOperatingProfit() != null
                && statement.getRevenue() != null
                && statement.getRevenue()
                        .compareTo(BigDecimal.ZERO) > 0
                && statement.getOperatingProfit()
                        .compareTo(statement.getRevenue()) > 0) {

            issues.add(new DataQualityIssue(
                    "OPERATING_PROFIT_EXCEEDS_REVENUE",
                    DataQualitySeverity.ERROR.name(),
                    statement.getFiscalYear(),
                    "OPERATING_PROFIT",
                    statement.getOperatingProfit(),
                    "below reported revenue",
                    "Operating profit cannot exceed revenue in "
                            + "conventional reporting. The figure "
                            + "likely includes non-operating income "
                            + "or belongs to a different reporting "
                            + "basis."
            ));
        }

        return issues;
    }

    private List<DataQualityIssue> checkChanges(
            FinancialStatement current,
            FinancialStatement previous
    ) {
        List<DataQualityIssue> issues = new ArrayList<>();

        checkChange(
                issues,
                current.getFiscalYear(),
                "REVENUE",
                current.getRevenue(),
                previous.getRevenue(),
                EXTREME_REVENUE_CHANGE,
                "Revenue rarely moves this much in one year "
                        + "without a merger, demerger or a change "
                        + "between standalone and consolidated "
                        + "reporting."
        );

        checkChange(
                issues,
                current.getFiscalYear(),
                "NET_INCOME",
                current.getNetIncome(),
                previous.getNetIncome(),
                EXTREME_CHANGE,
                "A change of this magnitude in net income is far "
                        + "more often a reporting inconsistency "
                        + "than a business event."
        );

        checkChange(
                issues,
                current.getFiscalYear(),
                "OPERATING_PROFIT",
                current.getOperatingProfit(),
                previous.getOperatingProfit(),
                EXTREME_CHANGE,
                "A change of this magnitude in operating profit "
                        + "suggests the two periods were not "
                        + "reported on the same basis."
        );

        return issues;
    }

    private void checkChange(
            List<DataQualityIssue> issues,
            Integer fiscalYear,
            String metric,
            BigDecimal currentValue,
            BigDecimal previousValue,
            BigDecimal threshold,
            String explanation
    ) {
        BigDecimal change = percentageChange(
                currentValue,
                previousValue
        );

        if (change == null) {
            return;
        }

        if (change.abs().compareTo(threshold) <= 0) {
            return;
        }

        issues.add(new DataQualityIssue(
                "EXTREME_YEAR_ON_YEAR_CHANGE",
                DataQualitySeverity.ERROR.name(),
                fiscalYear,
                metric,
                change,
                "within plus or minus " + threshold + " percent",
                explanation
        ));
    }

    /**
     * Percentage change between two periods.
     *
     * Uses the absolute previous value as the denominator so a move
     * from a loss toward profit registers as positive rather than
     * inverted.
     */
    private BigDecimal percentageChange(
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
                .divide(previousValue.abs(), 6,
                        RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
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

    private String buildSummary(
            int issueCount,
            int errorCount,
            boolean scoringAffected
    ) {
        if (issueCount == 0) {
            return "No data quality issues detected.";
        }

        if (scoringAffected) {
            return errorCount + " error level issue"
                    + (errorCount == 1 ? "" : "s")
                    + " affect the periods used for scoring. Any "
                    + "score for this company should be treated as "
                    + "unreliable until the source data is "
                    + "verified.";
        }

        return issueCount + " issue"
                + (issueCount == 1 ? "" : "s")
                + " detected in older periods. Current scoring "
                + "periods appear clean.";
    }
}
