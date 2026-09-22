package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.DataQualitySeverity;
import com.abhiram.atlas.domain.SectorMarginBand;
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
 * WHAT THE FIRST VERSION GOT WRONG
 *
 * A flat 40 percent margin ceiling caught HINDALCO correctly but
 * wrongly flagged POWERGRID at 44.53 percent, which is genuine for a
 * regulated transmission monopoly. Ceilings are now per sector.
 *
 * Percentage change was computed against any non-zero base, which
 * produced figures like BHARTIARTL's 47,839 percent operating profit
 * increase in FY2022. That number is arithmetically true and
 * analytically useless: it describes recovery from a near-zero base,
 * not a reporting inconsistency. Changes from a small base are now
 * reported as such rather than as an extreme change.
 *
 * DESIGN PRINCIPLE
 *
 * Checks never modify or discard data. Silently correcting a figure
 * would hide a provider problem and make stored history disagree
 * with its source. The checks annotate; callers decide.
 */
@Service
@Transactional(readOnly = true)
public class DataQualityService {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    /**
     * Net margin below this suggests severe distress or a reporting
     * problem. A warning rather than an error, because genuine large
     * losses do happen.
     */
    private static final BigDecimal SEVERE_LOSS_MARGIN =
            BigDecimal.valueOf(-50);

    /**
     * Year on year change beyond this is treated as a probable data
     * problem, but only when the base is large enough for the
     * percentage to mean anything.
     */
    private static final BigDecimal EXTREME_CHANGE =
            BigDecimal.valueOf(300);

    /**
     * Revenue gets a tighter bound because a top line rarely doubles
     * or halves without a merger or a change in reporting basis.
     */
    private static final BigDecimal EXTREME_REVENUE_CHANGE =
            BigDecimal.valueOf(60);

    /**
     * Minimum base size, as a fraction of revenue, for a percentage
     * change to be meaningful.
     *
     * BHARTIARTL FY2022 showed operating profit rising 47,839 percent
     * because the prior year was close to zero after AGR charges.
     * Below two percent of revenue, a percentage change describes
     * the smallness of the base rather than the size of the move.
     */
    private static final BigDecimal MINIMUM_BASE_FRACTION =
            BigDecimal.valueOf(0.02);

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

        String sector = company.getSector();

        List<FinancialStatement> statements =
                financialRepository
                        .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                companyId
                        );

        List<DataQualityIssue> issues = new ArrayList<>();

        for (FinancialStatement statement : statements) {
            issues.addAll(checkMargins(statement, sector));
        }

        for (int index = 0; index < statements.size() - 1; index++) {

            FinancialStatement current = statements.get(index);
            FinancialStatement previous = statements.get(index + 1);

            issues.addAll(checkChanges(current, previous));
        }

        long errorCount = issues.stream()
                .filter(issue -> DataQualitySeverity.ERROR.name()
                        .equals(issue.severity()))
                .count();

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

    private boolean affectsScoringPeriods(
            DataQualityIssue issue,
            List<FinancialStatement> statements
    ) {
        Integer currentYear = statements.get(0).getFiscalYear();
        Integer previousYear = statements.get(1).getFiscalYear();

        return issue.fiscalYear().equals(currentYear)
                || issue.fiscalYear().equals(previousYear);
    }

    private List<DataQualityIssue> checkMargins(
            FinancialStatement statement,
            String sector
    ) {
        List<DataQualityIssue> issues = new ArrayList<>();

        BigDecimal netMargin = percentage(
                statement.getNetIncome(),
                statement.getRevenue()
        );

        if (netMargin == null) {
            return issues;
        }

        BigDecimal ceiling = SectorMarginBand.ceilingFor(sector);

        if (netMargin.compareTo(ceiling) > 0) {

            issues.add(new DataQualityIssue(
                    "IMPLAUSIBLE_NET_MARGIN",
                    DataQualitySeverity.ERROR.name(),
                    statement.getFiscalYear(),
                    "NET_MARGIN",
                    netMargin,
                    "below " + ceiling + " percent for "
                            + describeSector(sector),
                    "A net margin this high is not plausible for "
                            + describeSector(sector)
                            + ". The reported net income may not be "
                            + "net income, or the period may mix "
                            + "consolidated and standalone figures."
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
                            + "from this period."
            ));
        }

        if (exceedsRevenue(
                statement.getOperatingProfit(),
                statement.getRevenue())) {

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
                previous.getRevenue(),
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
                previous.getRevenue(),
                EXTREME_CHANGE,
                "A change of this magnitude in operating profit "
                        + "suggests the two periods were not "
                        + "reported on the same basis."
        );

        return issues;
    }

    /**
     * Evaluates a year on year change.
     *
     * When the previous value is small relative to revenue, the
     * percentage is suppressed and a low base note is recorded
     * instead. A move from near zero to a normal figure produces a
     * huge percentage that describes the base, not the move, and
     * reporting it as an extreme change would be misleading.
     */
    private void checkChange(
            List<DataQualityIssue> issues,
            Integer fiscalYear,
            String metric,
            BigDecimal currentValue,
            BigDecimal previousValue,
            BigDecimal previousRevenue,
            BigDecimal threshold,
            String explanation
    ) {
        if (currentValue == null || previousValue == null) {
            return;
        }

        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        if (isLowBase(previousValue, previousRevenue)) {

            // Only worth noting when the move is large. A small
            // change from a small base is unremarkable.
            if (isMateriallyDifferent(
                    currentValue,
                    previousValue,
                    previousRevenue)) {

                issues.add(new DataQualityIssue(
                        "CHANGE_FROM_LOW_BASE",
                        DataQualitySeverity.WARNING.name(),
                        fiscalYear,
                        metric,
                        previousValue,
                        "a base above "
                                + MINIMUM_BASE_FRACTION
                                        .multiply(ONE_HUNDRED)
                                + " percent of revenue",
                        "The prior period value was close to zero, "
                                + "so a percentage change is not "
                                + "meaningful. Compare the absolute "
                                + "figures instead."
                ));
            }

            return;
        }

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
     * True when the base is too small for a percentage to be
     * informative.
     *
     * Revenue is used as the yardstick because it is the most stable
     * figure on the statement and does not collapse when profit does.
     */
    private boolean isLowBase(
            BigDecimal previousValue,
            BigDecimal previousRevenue
    ) {
        if (previousRevenue == null
                || previousRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal floor = previousRevenue
                .multiply(MINIMUM_BASE_FRACTION);

        return previousValue.abs().compareTo(floor) < 0;
    }

    private boolean isMateriallyDifferent(
            BigDecimal currentValue,
            BigDecimal previousValue,
            BigDecimal previousRevenue
    ) {
        if (previousRevenue == null
                || previousRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal movement =
                currentValue.subtract(previousValue).abs();

        BigDecimal floor = previousRevenue
                .multiply(MINIMUM_BASE_FRACTION);

        return movement.compareTo(floor) > 0;
    }

    private boolean exceedsRevenue(
            BigDecimal operatingProfit,
            BigDecimal revenue
    ) {
        return operatingProfit != null
                && revenue != null
                && revenue.compareTo(BigDecimal.ZERO) > 0
                && operatingProfit.compareTo(revenue) > 0;
    }

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

    private String describeSector(String sector) {
        return sector == null || sector.isBlank()
                ? "an unclassified company"
                : "the " + sector + " sector";
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
                + " detected, none affecting the periods used for "
                + "scoring.";
    }
}
