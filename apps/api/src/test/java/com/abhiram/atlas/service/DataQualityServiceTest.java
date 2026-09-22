package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.DataQualityReport;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for financial data quality detection.
 *
 * The HINDALCO fixtures are the real figures that exposed the gap.
 * FY2025 reported net income of 198,699 crore on revenue of 476,992
 * crore, a 42 percent net margin for an aluminium business whose
 * other years sit between 3 and 7 percent. Atlas scored the company
 * 8 out of 100 by comparing a normal year against that one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataQualityServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FinancialStatementRepository financialRepository;

    @InjectMocks
    private DataQualityService service;

    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        company = newCompany();

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));
    }

    @Test
    @DisplayName("Detects the HINDALCO reporting anomaly")
    void detectsHindalcoAnomaly() throws Exception {

        // Real figures from the provider. FY2025 is corrupted.
        stub(
                statement(2026, "274944.0", "13391.0", "22300.0"),
                statement(2025, "476992.0", "198699.0", "210000.0"),
                statement(2024, "215962.0", "10155.0", "17500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.errorCount()).isGreaterThan(0);
        assertThat(report.reliableForScoring()).isFalse();

        assertThat(report.issues())
                .extracting("check")
                .contains("IMPLAUSIBLE_NET_MARGIN");

        assertThat(report.summary())
                .containsIgnoringCase("unreliable");
    }

    @Test
    @DisplayName("Flags a net margin no operating company achieves")
    void flagsImplausibleMargin() throws Exception {

        // 198699 / 476992 is roughly 41.7 percent.
        stub(
                statement(2025, "476992.0", "198699.0", "210000.0"),
                statement(2024, "215962.0", "10155.0", "17500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .filteredOn("check", "IMPLAUSIBLE_NET_MARGIN")
                .isNotEmpty()
                .first()
                .satisfies(issue -> {
                    assertThat(issue.severity())
                            .isEqualTo("ERROR");
                    assertThat(issue.fiscalYear())
                            .isEqualTo(2025);
                });
    }

    @Test
    @DisplayName("Flags an impossible year on year jump")
    void flagsExtremeChange() throws Exception {

        // Net income moving from 10155 to 198699 is roughly a
        // 1857 percent increase.
        stub(
                statement(2025, "476992.0", "198699.0", "210000.0"),
                statement(2024, "215962.0", "10155.0", "17500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .filteredOn(
                        "check",
                        "EXTREME_YEAR_ON_YEAR_CHANGE")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Flags operating profit above revenue")
    void flagsOperatingProfitAboveRevenue() throws Exception {

        stub(
                statement(2026, "100000.0", "15000.0", "120000.0"),
                statement(2025, "95000.0", "14000.0", "18000.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .extracting("check")
                .contains("OPERATING_PROFIT_EXCEEDS_REVENUE");
    }

    @Test
    @DisplayName("Passes a clean history without complaint")
    void passesCleanHistory() throws Exception {

        // Real INFY figures. Steady growth, sensible margins.
        stub(
                statement(2026, "178650.0", "29440.0", "36254.0"),
                statement(2025, "162990.0", "26713.0", "34424.0"),
                statement(2024, "153670.0", "26233.0", "31747.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issueCount()).isZero();
        assertThat(report.reliableForScoring()).isTrue();
        assertThat(report.summary())
                .containsIgnoringCase("no data quality issues");
    }

    @Test
    @DisplayName("Tolerates ordinary cyclical swings")
    void toleratesCyclicalSwings() throws Exception {

        // Real TATASTEEL style volatility. Large but genuine.
        stub(
                statement(2026, "218543.0", "3174.0", "21000.0"),
                statement(2025, "229171.0", "4910.0", "25000.0"),
                statement(2024, "242000.0", "8000.0", "30000.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        // A check that fires on ordinary volatility trains you to
        // ignore it, which is worse than no check.
        assertThat(report.reliableForScoring()).isTrue();
    }

        @Test
    @DisplayName("Does not penalise scoring for old period issues")
    void oldIssuesDoNotAffectScoring() throws Exception {

        // The corrupted margin sits in FY2022, outside the two
        // periods scoring uses. Intervening years keep the
        // year on year checks from firing on a scoring period.
        stub(
                statement(2026, "178650.0", "29440.0", "36254.0"),
                statement(2025, "162990.0", "26713.0", "34424.0"),
                statement(2024, "153670.0", "26233.0", "31747.0"),
                statement(2023, "146767.0", "24095.0", "30905.0"),
                statement(2022, "121641.0", "80000.0", "28015.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.errorCount()).isGreaterThan(0);

        // FY2022 is corrupted but scoring uses FY2026 and FY2025.
        assertThat(report.reliableForScoring()).isTrue();
        assertThat(report.summary())
                .containsIgnoringCase("older periods");
    }

    @Test
    @DisplayName("Handles missing figures without firing")
    void handlesMissingFigures() throws Exception {

        stub(
                statement(2026, null, null, null),
                statement(2025, "162990.0", "26713.0", "34424.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        // Absent data is a coverage gap, not a quality violation.
        // Flagging it here would duplicate what the scoring engine
        // already reports.
        assertThat(report.errorCount()).isZero();
    }

    @Test
    @DisplayName("Handles a company with no statements")
    void handlesEmptyHistory() {

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of());

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.periodsChecked()).isZero();
        assertThat(report.issueCount()).isZero();
        assertThat(report.reliableForScoring()).isTrue();
    }

    // ----------------------------------------------------------
    // Fixtures
    // ----------------------------------------------------------

    private void stub(FinancialStatement... statements) {
        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(statements));
    }

    private FinancialStatement statement(
            int fiscalYear,
            String revenue,
            String netIncome,
            String operatingProfit
    ) throws Exception {

        FinancialStatement statement =
                newInstance(FinancialStatement.class);

        setField(statement, "id", UUID.randomUUID());
        setField(statement, "company", company);
        setField(statement, "fiscalYear", fiscalYear);
        setField(statement, "fiscalQuarter", 4);
        setField(statement, "revenue", decimal(revenue));
        setField(statement, "netIncome", decimal(netIncome));
        setField(
                statement,
                "operatingProfit",
                decimal(operatingProfit));
        setField(
                statement,
                "operatingCashFlow",
                decimal(netIncome));
        setField(statement, "createdAt", LocalDateTime.now());
        setField(statement, "updatedAt", LocalDateTime.now());

        return statement;
    }

    private Company newCompany() throws Exception {

        Instrument instrument = newInstance(Instrument.class);

        setField(instrument, "id", UUID.randomUUID());
        setField(instrument, "symbol", "HINDALCO");
        setField(
                instrument,
                "companyName",
                "Hindalco Industries Ltd");
        setField(instrument, "exchange", "NSE");
        setField(instrument, "active", Boolean.TRUE);
        setField(instrument, "createdAt", LocalDateTime.now());
        setField(instrument, "updatedAt", LocalDateTime.now());

        Company newCompany = newInstance(Company.class);

        setField(newCompany, "id", COMPANY_ID);
        setField(newCompany, "instrument", instrument);
        setField(newCompany, "sector", "Metals and Mining");
        setField(newCompany, "industry", "Aluminium");
        setField(newCompany, "website", "https://example.com");
        setField(newCompany, "description", "Fixture");
        setField(
                newCompany,
                "scoringProfile",
                "OPERATING_COMPANY");
        setField(newCompany, "createdAt", LocalDateTime.now());
        setField(newCompany, "updatedAt", LocalDateTime.now());

        return newCompany;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private <T> T newInstance(Class<T> type) throws Exception {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}
