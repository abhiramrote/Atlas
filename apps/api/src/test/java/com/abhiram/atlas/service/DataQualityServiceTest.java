package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.DataQualityReport;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

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
 * Fixtures use real figures from the three cases that shaped this
 * feature:
 *
 *   HINDALCO    corrupted FY2025, 41.66 percent margin on aluminium
 *   POWERGRID   genuine 44.53 percent margin on regulated transmission
 *   BHARTIARTL  47,839 percent operating profit change from a
 *               near-zero base after AGR charges
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

    // ----------------------------------------------------------
    // Sector aware margin ceilings
    // ----------------------------------------------------------

    @Test
    @DisplayName("Flags an impossible margin for a metals company")
    void flagsImpossibleMetalsMargin() throws Exception {

        // HINDALCO FY2025. 198699 / 476992 is 41.66 percent, far
        // above anything an aluminium business achieves.
        setUpCompany("HINDALCO", "Metals and Mining");

        stub(
                statement(2025, "476992.0", "198699.0", "210000.0"),
                statement(2024, "215962.0", "10155.0", "17500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .extracting("check")
                .contains("IMPLAUSIBLE_NET_MARGIN");

        assertThat(report.reliableForScoring()).isFalse();
    }

    @Test
    @DisplayName("Accepts a high margin for regulated transmission")
    void acceptsRegulatedPowerMargin() throws Exception {

        // POWERGRID genuinely earns mid forties because it is a
        // regulated monopoly with guaranteed returns. A flat
        // ceiling wrongly flagged this.
        setUpCompany("POWERGRID", "Power");

        stub(
                statement(2026, "45000.0", "20038.0", "28000.0"),
                statement(2025, "44000.0", "19500.0", "27500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .extracting("check")
                .doesNotContain("IMPLAUSIBLE_NET_MARGIN");

        assertThat(report.reliableForScoring()).isTrue();
    }

    @Test
    @DisplayName("Still flags an impossible margin within Power")
    void flagsImpossibleMarginEvenInPower() throws Exception {

        // The Power ceiling is generous, not absent.
        setUpCompany("POWERGRID", "Power");

        stub(
                statement(2026, "45000.0", "31000.0", "33000.0"),
                statement(2025, "44000.0", "19500.0", "27500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .extracting("check")
                .contains("IMPLAUSIBLE_NET_MARGIN");
    }

    @Test
    @DisplayName("Applies the default ceiling to unknown sectors")
    void appliesDefaultCeiling() throws Exception {

        setUpCompany("UNKNOWN", "Some New Sector");

        stub(
                statement(2026, "100000.0", "45000.0", "50000.0"),
                statement(2025, "95000.0", "42000.0", "47000.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .extracting("check")
                .contains("IMPLAUSIBLE_NET_MARGIN");
    }

    // ----------------------------------------------------------
    // Low base handling
    // ----------------------------------------------------------

    @Test
    @DisplayName("Does not report an extreme change from a low base")
    void suppressesExtremeChangeFromLowBase() throws Exception {

        // BHARTIARTL FY2022. Operating profit rose from near zero
        // after AGR charges, producing 47,839 percent. True, and
        // analytically useless.
        setUpCompany("BHARTIARTL", "Telecommunications");

        stub(
                statement(2022, "116547.0", "4255.0", "48000.0"),
                statement(2021, "100616.0", "-15084.0", "100.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .filteredOn("metric", "OPERATING_PROFIT")
                .extracting("check")
                .doesNotContain("EXTREME_YEAR_ON_YEAR_CHANGE");
    }

    @Test
    @DisplayName("Reports a low base as a warning, not an error")
    void reportsLowBaseAsWarning() throws Exception {

        setUpCompany("BHARTIARTL", "Telecommunications");

        stub(
                statement(2022, "116547.0", "4255.0", "48000.0"),
                statement(2021, "100616.0", "-15084.0", "100.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .filteredOn("check", "CHANGE_FROM_LOW_BASE")
                .isNotEmpty()
                .allSatisfy(issue ->
                        assertThat(issue.severity())
                                .isEqualTo("WARNING"));
    }

    @Test
    @DisplayName("A low base warning does not block scoring")
    void lowBaseDoesNotBlockScoring() throws Exception {

        setUpCompany("BHARTIARTL", "Telecommunications");

        stub(
                statement(2026, "172985.0", "33556.0", "60000.0"),
                statement(2025, "149307.0", "33000.0", "100.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        // A near-zero prior year is a limitation of the comparison,
        // not evidence that the current figures are wrong.
        assertThat(report.reliableForScoring()).isTrue();
    }

    @Test
    @DisplayName("Still flags an extreme change from a real base")
    void flagsExtremeChangeFromRealBase() throws Exception {

        // HINDALCO net income moving from 10155 to 198699. The base
        // is 4.7 percent of revenue, comfortably above the floor.
        setUpCompany("HINDALCO", "Metals and Mining");

        stub(
                statement(2025, "476992.0", "198699.0", "210000.0"),
                statement(2024, "215962.0", "10155.0", "17500.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issues())
                .filteredOn("metric", "NET_INCOME")
                .extracting("check")
                .contains("EXTREME_YEAR_ON_YEAR_CHANGE");
    }

    // ----------------------------------------------------------
    // Existing behaviour
    // ----------------------------------------------------------

    @Test
    @DisplayName("Flags operating profit above revenue")
    void flagsOperatingProfitAboveRevenue() throws Exception {

        setUpCompany("TEST", "Consumer Goods");

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

        // Real INFY figures.
        setUpCompany("INFY", "Information Technology");

        stub(
                statement(2026, "178650.0", "29440.0", "36254.0"),
                statement(2025, "162990.0", "26713.0", "34424.0"),
                statement(2024, "153670.0", "26233.0", "31747.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.issueCount()).isZero();
        assertThat(report.reliableForScoring()).isTrue();
    }

    @Test
    @DisplayName("Tolerates ordinary cyclical swings")
    void toleratesCyclicalSwings() throws Exception {

        setUpCompany("TATASTEEL", "Metals and Mining");

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

        setUpCompany("INFY", "Information Technology");

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
                .containsIgnoringCase("none affecting");
    }

    @Test
    @DisplayName("Handles missing figures without firing")
    void handlesMissingFigures() throws Exception {

        setUpCompany("TEST", "Consumer Goods");

        stub(
                statement(2026, null, null, null),
                statement(2025, "162990.0", "26713.0", "34424.0")
        );

        DataQualityReport report = service.check(COMPANY_ID);

        // Absent data is a coverage gap, not a quality violation.
        assertThat(report.errorCount()).isZero();
    }

    @Test
    @DisplayName("Handles a company with no statements")
    void handlesEmptyHistory() throws Exception {

        setUpCompany("TEST", "Consumer Goods");

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of());

        DataQualityReport report = service.check(COMPANY_ID);

        assertThat(report.periodsChecked()).isZero();
        assertThat(report.reliableForScoring()).isTrue();
    }

    // ----------------------------------------------------------
    // Fixtures
    // ----------------------------------------------------------

    private Company company;

    private void setUpCompany(String symbol, String sector)
            throws Exception {

        Instrument instrument = newInstance(Instrument.class);

        setField(instrument, "id", UUID.randomUUID());
        setField(instrument, "symbol", symbol);
        setField(instrument, "companyName", symbol + " Ltd");
        setField(instrument, "exchange", "NSE");
        setField(instrument, "active", Boolean.TRUE);
        setField(instrument, "createdAt", LocalDateTime.now());
        setField(instrument, "updatedAt", LocalDateTime.now());

        company = newInstance(Company.class);

        setField(company, "id", COMPANY_ID);
        setField(company, "instrument", instrument);
        setField(company, "sector", sector);
        setField(company, "industry", "Test Industry");
        setField(company, "website", "https://example.com");
        setField(company, "description", "Fixture");
        setField(
                company,
                "scoringProfile",
                "OPERATING_COMPANY");
        setField(company, "createdAt", LocalDateTime.now());
        setField(company, "updatedAt", LocalDateTime.now());

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));
    }

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
