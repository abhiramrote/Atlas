package com.abhiram.atlas.provider;

import com.abhiram.atlas.config.IndianApiConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for IndianAPI parsing and period extraction.
 *
 * Fixtures mirror the real INFY response, including the mix of
 * annual and interim periods that must be separated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndianApiProviderTest {

    @Mock
    private RestClient restClient;

    @Mock
    private IndianApiConfig config;

    private IndianApiProvider provider;

    @BeforeEach
    void setUp() {
        provider = new IndianApiProvider(restClient, config);
    }

    // ----------------------------------------------------------
    // Period extraction
    // ----------------------------------------------------------

    @Test
    @DisplayName("Extracts annual periods and discards interim ones")
    void extractsOnlyAnnualPeriods() {

        IndianApiCompanyResponse response = response(
                annual("2026", "2026-03-31", "178650.00",
                        "29440.00", "36254.00", "33986.00"),
                annual("2025", "2025-03-31", "162990.00",
                        "26713.00", "34011.00", "31284.00"),
                interim("2026", "2025-12-31", "44490.00",
                        "6806.00", "9120.00", "8100.00"),
                interim("2026", "2025-09-30", "44490.00",
                        "7364.00", "9200.00", "8300.00")
        );

        List<FinancialPeriodData> periods =
                provider.extractAnnualPeriods(response);

        // Mixing a quarter into the annual series would make year on
        // year growth compare three months against twelve.
        assertThat(periods).hasSize(2);

        assertThat(periods)
                .extracting(FinancialPeriodData::fiscalYear)
                .containsExactly(2026, 2025);
    }

    @Test
    @DisplayName("Sorts periods newest first")
    void sortsNewestFirst() {

        IndianApiCompanyResponse response = response(
                annual("2024", "2024-03-31", "153670.00",
                        "26233.00", "32133.00", "25210.00"),
                annual("2026", "2026-03-31", "178650.00",
                        "29440.00", "36254.00", "33986.00"),
                annual("2025", "2025-03-31", "162990.00",
                        "26713.00", "34011.00", "31284.00")
        );

        List<FinancialPeriodData> periods =
                provider.extractAnnualPeriods(response);

        assertThat(periods)
                .extracting(FinancialPeriodData::fiscalYear)
                .containsExactly(2026, 2025, 2024);
    }

    @Test
    @DisplayName("Reads figures from the income and cash flow maps")
    void readsFiguresFromStatementMaps() {

        IndianApiCompanyResponse response = response(
                annual("2026", "2026-03-31", "178650.00",
                        "29440.00", "36254.00", "33986.00")
        );

        FinancialPeriodData period =
                provider.extractAnnualPeriods(response).get(0);

        assertThat(period.revenue())
                .isEqualByComparingTo("178650.00");

        assertThat(period.netIncome())
                .isEqualByComparingTo("29440.00");

        assertThat(period.operatingProfit())
                .isEqualByComparingTo("36254.00");

        assertThat(period.operatingCashFlow())
                .isEqualByComparingTo("33986.00");

        assertThat(period.fiscalQuarter()).isEqualTo(4);
    }

    @Test
    @DisplayName("Falls back to TotalRevenue when Revenue is absent")
    void fallsBackToTotalRevenue() {

        IndianApiFinancialMap map = new IndianApiFinancialMap(
                List.of(
                        item("TotalRevenue", "99000.00"),
                        item("NetIncome", "12000.00")
                ),
                List.of(),
                List.of()
        );

        IndianApiCompanyResponse response = response(
                new IndianApiFinancialPeriod(
                        "2026", "2026-03-31", "Annual", 0, map
                )
        );

        FinancialPeriodData period =
                provider.extractAnnualPeriods(response).get(0);

        assertThat(period.revenue())
                .isEqualByComparingTo("99000.00");
    }

    @Test
    @DisplayName("Keeps a period that is only partially reported")
    void keepsPartiallyReportedPeriod() {

        IndianApiFinancialMap map = new IndianApiFinancialMap(
                List.of(item("Revenue", "50000.00")),
                List.of(),
                List.of()
        );

        IndianApiCompanyResponse response = response(
                new IndianApiFinancialPeriod(
                        "2026", "2026-03-31", "Annual", 0, map
                )
        );

        List<FinancialPeriodData> periods =
                provider.extractAnnualPeriods(response);

        // Partial data is still useful. Discarding the period would
        // lose a real revenue figure.
        assertThat(periods).hasSize(1);
        assertThat(periods.get(0).netIncome()).isNull();
    }

    @Test
    @DisplayName("Discards a period with no figures at all")
    void discardsEmptyPeriod() {

        IndianApiFinancialMap map = new IndianApiFinancialMap(
                List.of(),
                List.of(),
                List.of()
        );

        IndianApiCompanyResponse response = response(
                new IndianApiFinancialPeriod(
                        "2026", "2026-03-31", "Annual", 0, map
                )
        );

        assertThat(provider.extractAnnualPeriods(response))
                .isEmpty();
    }

    @Test
    @DisplayName("Handles a missing financials list")
    void handlesMissingFinancials() {

        IndianApiCompanyResponse response =
                new IndianApiCompanyResponse(
                        "Infosys", "Software", null, null, null
                );

        assertThat(provider.extractAnnualPeriods(response))
                .isEmpty();
    }

    @Test
    @DisplayName("Falls back to EndDate when FiscalYear is unusable")
    void resolvesYearFromEndDate() {

        IndianApiFinancialMap map = new IndianApiFinancialMap(
                List.of(item("Revenue", "50000.00")),
                List.of(),
                List.of()
        );

        IndianApiCompanyResponse response = response(
                new IndianApiFinancialPeriod(
                        null, "2025-03-31", "Annual", 0, map
                )
        );

        assertThat(provider.extractAnnualPeriods(response))
                .first()
                .extracting(FinancialPeriodData::fiscalYear)
                .isEqualTo(2025);
    }

    // ----------------------------------------------------------
    // Numeric parsing
    // ----------------------------------------------------------

    @Test
    @DisplayName("Parses a plain decimal")
    void parsesPlainDecimal() {
        assertThat(provider.parseAmount("178650.00"))
                .isEqualByComparingTo("178650.00");
    }

    @Test
    @DisplayName("Parses Indian thousands separators")
    void parsesIndianSeparators() {

        assertThat(provider.parseAmount("1,04,545.00"))
                .isEqualByComparingTo("104545.00");

        assertThat(provider.parseAmount("12,34,56,789"))
                .isEqualByComparingTo("123456789");
    }

    @Test
    @DisplayName("Strips currency decoration")
    void stripsCurrencyDecoration() {

        assertThat(provider.parseAmount("\u20B91,234.50"))
                .isEqualByComparingTo("1234.50");

        assertThat(provider.parseAmount("Rs. 9,876"))
                .isEqualByComparingTo("9876");
    }

    @Test
    @DisplayName("Reads parenthesised values as negative")
    void readsAccountingNegatives() {

        assertThat(provider.parseAmount("(2,824.00)"))
                .isEqualByComparingTo("-2824.00");
    }

    @Test
    @DisplayName("Parses an explicit negative")
    void parsesExplicitNegative() {

        // The live cash flow section returns values like -39786.
        assertThat(provider.parseAmount("-39786"))
                .isEqualByComparingTo("-39786");
    }

    @Test
    @DisplayName("Never returns zero for missing data")
    void neverReturnsZeroForMissingData() {

        String[] missing = {
                null, "", "   ", "-", "NA", "N/A",
                "null", "--", "not a number", "12.34.56"
        };

        for (String value : missing) {

            BigDecimal result = provider.parseAmount(value);

            assertThat(result)
                    .as("value '%s' must not parse to zero", value)
                    .isNull();
        }
    }

    @Test
    @DisplayName("Preserves a genuine zero")
    void preservesGenuineZero() {

        assertThat(provider.parseAmount("0.00"))
                .isEqualByComparingTo("0.00");
    }

    // ----------------------------------------------------------
    // Configuration guards
    // ----------------------------------------------------------

    @Test
    @DisplayName("Refuses to fetch when not configured")
    void refusesWhenNotConfigured() {

        when(config.isUsable()).thenReturn(false);

        assertThatThrownBy(() ->
                provider.fetchFundamentals("INFY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("Exposes a stable provider name")
    void exposesProviderName() {
        assertThat(provider.getProviderName())
                .isEqualTo("INDIAN_API");
    }

    // ----------------------------------------------------------
    // Fixtures
    // ----------------------------------------------------------

    private IndianApiCompanyResponse response(
            IndianApiFinancialPeriod... periods
    ) {
        return new IndianApiCompanyResponse(
                "Infosys",
                "Software & Programming",
                new IndianApiCompanyProfile(
                        "Test description",
                        "Software & Programming",
                        "INE009A01021"
                ),
                null,
                List.of(periods)
        );
    }

    private IndianApiFinancialPeriod annual(
            String fiscalYear,
            String endDate,
            String revenue,
            String netIncome,
            String operatingIncome,
            String operatingCashFlow
    ) {
        return period(
                fiscalYear, endDate, "Annual",
                revenue, netIncome,
                operatingIncome, operatingCashFlow
        );
    }

    private IndianApiFinancialPeriod interim(
            String fiscalYear,
            String endDate,
            String revenue,
            String netIncome,
            String operatingIncome,
            String operatingCashFlow
    ) {
        return period(
                fiscalYear, endDate, "Interim",
                revenue, netIncome,
                operatingIncome, operatingCashFlow
        );
    }

    private IndianApiFinancialPeriod period(
            String fiscalYear,
            String endDate,
            String type,
            String revenue,
            String netIncome,
            String operatingIncome,
            String operatingCashFlow
    ) {
        IndianApiFinancialMap map = new IndianApiFinancialMap(
                List.of(
                        item("Revenue", revenue),
                        item("TotalRevenue", revenue),
                        item("NetIncome", netIncome),
                        item("OperatingIncome", operatingIncome)
                ),
                List.of(),
                List.of(
                        item(
                                "CashfromOperatingActivities",
                                operatingCashFlow
                        )
                )
        );

        return new IndianApiFinancialPeriod(
                fiscalYear, endDate, type, 0, map
        );
    }

    private IndianApiLineItem item(String key, String value) {
        return new IndianApiLineItem(key, key, value);
    }
}
