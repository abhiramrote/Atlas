package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.OpportunityScoreResponse;
import com.abhiram.atlas.dto.ScoreFactorResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the transparent scoring engine.
 *
 * These tests use mocked repositories so no database is required.
 */
@ExtendWith(MockitoExtension.class)
class OpportunityScoreServiceTest {

    private static final UUID COMPANY_ID =
            UUID.fromString("650e8400-e29b-41d4-a716-446655440001");

    private static final UUID INSTRUMENT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FinancialStatementRepository financialRepository;

    @InjectMocks
    private OpportunityScoreService service;

    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        Instrument instrument = newInstrument(
                INSTRUMENT_ID,
                "RELIANCE",
                "Reliance Industries Ltd",
                "NSE"
        );

        company = newCompany(COMPANY_ID, instrument);
    }

    @Test
    @DisplayName("Scores a company using the two most recent periods")
    void scoresCompanyUsingTwoMostRecentPeriods() throws Exception {

        FinancialStatement current = newStatement(
                2025,
                "1000000.00",
                "150000.00",
                "220000.00",
                "180000.00"
        );

        FinancialStatement previous = newStatement(
                2024,
                "900000.00",
                "130000.00",
                "190000.00",
                "150000.00"
        );

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(current, previous));

        OpportunityScoreResponse response =
                service.scoreCompany(COMPANY_ID);

        assertThat(response.symbol()).isEqualTo("RELIANCE");
        assertThat(response.companyName())
                .isEqualTo("Reliance Industries Ltd");

        assertThat(response.currentFiscalYear()).isEqualTo(2025);
        assertThat(response.previousFiscalYear()).isEqualTo(2024);

        assertThat(response.maximumScore()).isEqualTo(100);
        assertThat(response.policyVersion()).isNotBlank();

        assertThat(response.factors()).hasSize(6);

        assertThat(response.score())
                .isBetween(0, response.maximumScore());
    }

    @Test
    @DisplayName("Awards full points when growth is at least fifteen percent")
    void awardsFullPointsForStrongGrowth() throws Exception {

        // Net income grows from 100000 to 130000 which is 30 percent.
        FinancialStatement current = newStatement(
                2025,
                "1000000.00",
                "130000.00",
                "200000.00",
                "180000.00"
        );

        FinancialStatement previous = newStatement(
                2024,
                "800000.00",
                "100000.00",
                "150000.00",
                "140000.00"
        );

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(current, previous));

        OpportunityScoreResponse response =
                service.scoreCompany(COMPANY_ID);

        ScoreFactorResponse netIncomeGrowth = findFactor(
                response,
                "Net Income Growth"
        );

        assertThat(netIncomeGrowth.value())
                .isEqualByComparingTo("30.00");

        assertThat(netIncomeGrowth.pointsAwarded())
                .isEqualTo(netIncomeGrowth.maximumPoints());
    }

    @Test
    @DisplayName("Awards zero points when a metric declines")
    void awardsZeroPointsWhenMetricDeclines() throws Exception {

        // Revenue falls from 1000000 to 900000.
        FinancialStatement current = newStatement(
                2025,
                "900000.00",
                "100000.00",
                "150000.00",
                "120000.00"
        );

        FinancialStatement previous = newStatement(
                2024,
                "1000000.00",
                "95000.00",
                "140000.00",
                "115000.00"
        );

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(current, previous));

        OpportunityScoreResponse response =
                service.scoreCompany(COMPANY_ID);

        ScoreFactorResponse revenueGrowth = findFactor(
                response,
                "Revenue Growth"
        );

        assertThat(revenueGrowth.value())
                .isEqualByComparingTo("-10.00");

        assertThat(revenueGrowth.pointsAwarded()).isZero();
    }

    @Test
    @DisplayName("Reports missing data instead of inventing a value")
    void reportsMissingDataWhenMetricIsNull() throws Exception {

        FinancialStatement current = newStatement(
                2025,
                "1000000.00",
                "150000.00",
                "220000.00",
                null
        );

        FinancialStatement previous = newStatement(
                2024,
                "900000.00",
                "130000.00",
                "190000.00",
                null
        );

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(current, previous));

        OpportunityScoreResponse response =
                service.scoreCompany(COMPANY_ID);

        ScoreFactorResponse cashFlowGrowth = findFactor(
                response,
                "Operating Cash Flow Growth"
        );

        assertThat(cashFlowGrowth.value()).isNull();
        assertThat(cashFlowGrowth.pointsAwarded()).isZero();
        assertThat(cashFlowGrowth.explanation())
                .containsIgnoringCase("unavailable");
    }

    @Test
    @DisplayName("Fails when the company does not exist")
    void failsWhenCompanyIsMissing() {

        when(companyRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.scoreCompany(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    @DisplayName("Fails when fewer than two financial periods exist")
    void failsWhenOnlyOnePeriodExists() throws Exception {

        FinancialStatement onlyPeriod = newStatement(
                2025,
                "1000000.00",
                "150000.00",
                "220000.00",
                "180000.00"
        );

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(onlyPeriod));

        assertThatThrownBy(() ->
                service.scoreCompany(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("two financial periods");
    }

    @Test
    @DisplayName("Ranks companies from highest to lowest score")
    void ranksCompaniesByScoreDescending() throws Exception {

        UUID secondCompanyId = UUID.randomUUID();

        Instrument weakInstrument = newInstrument(
                UUID.randomUUID(),
                "WEAK",
                "Weak Company Ltd",
                "NSE"
        );

        Company weakCompany =
                newCompany(secondCompanyId, weakInstrument);

        when(companyRepository.findAll())
                .thenReturn(List.of(weakCompany, company));

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(companyRepository.findById(secondCompanyId))
                .thenReturn(Optional.of(weakCompany));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        COMPANY_ID))
                .thenReturn(List.of(
                        newStatement(
                                2025,
                                "1000000.00",
                                "200000.00",
                                "250000.00",
                                "220000.00"
                        ),
                        newStatement(
                                2024,
                                "700000.00",
                                "120000.00",
                                "150000.00",
                                "130000.00"
                        )
                ));

        when(financialRepository
                .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                        secondCompanyId))
                .thenReturn(List.of(
                        newStatement(
                                2025,
                                "500000.00",
                                "10000.00",
                                "15000.00",
                                "12000.00"
                        ),
                        newStatement(
                                2024,
                                "600000.00",
                                "20000.00",
                                "30000.00",
                                "25000.00"
                        )
                ));

        List<OpportunityScoreResponse> ranked =
                service.getRankedOpportunities(10);

        assertThat(ranked).hasSize(2);

        assertThat(ranked.get(0).score())
                .isGreaterThanOrEqualTo(ranked.get(1).score());

        assertThat(ranked.get(0).symbol()).isEqualTo("RELIANCE");
    }

    @Test
    @DisplayName("Rejects an invalid discovery limit")
    void rejectsInvalidLimit() {

        assertThatThrownBy(() ->
                service.getRankedOpportunities(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be between");

        assertThatThrownBy(() ->
                service.getRankedOpportunities(500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limit must be between");
    }

    // ----------------------------------------------------------
    // Test fixture helpers
    //
    // Atlas entities intentionally expose no public setters, so
    // reflection is used to build immutable-looking test data.
    // ----------------------------------------------------------

    private ScoreFactorResponse findFactor(
            OpportunityScoreResponse response,
            String name
    ) {
        return response.factors()
                .stream()
                .filter(factor -> factor.name().equals(name))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Factor not found: " + name));
    }

    private Instrument newInstrument(
            UUID id,
            String symbol,
            String companyName,
            String exchange
    ) throws Exception {

        Instrument instrument = newInstance(Instrument.class);

        setField(instrument, "id", id);
        setField(instrument, "symbol", symbol);
        setField(instrument, "companyName", companyName);
        setField(instrument, "exchange", exchange);
        setField(instrument, "active", Boolean.TRUE);
        setField(instrument, "createdAt", LocalDateTime.now());
        setField(instrument, "updatedAt", LocalDateTime.now());

        return instrument;
    }

        private Company newCompany(
            UUID id,
            Instrument instrument
    ) throws Exception {

        Company newCompany = newInstance(Company.class);

        setField(newCompany, "id", id);
        setField(newCompany, "instrument", instrument);
        setField(newCompany, "sector", "Energy");
        setField(newCompany, "industry", "Diversified");
        setField(newCompany, "website", "https://example.com");
        setField(newCompany, "description", "Test fixture");
        setField(
                newCompany,
                "scoringProfile",
                "OPERATING_COMPANY"
        );
        setField(newCompany, "createdAt", LocalDateTime.now());
        setField(newCompany, "updatedAt", LocalDateTime.now());

        return newCompany;
    }

    private FinancialStatement newStatement(
            Integer fiscalYear,
            String revenue,
            String netIncome,
            String operatingProfit,
            String operatingCashFlow
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
                decimal(operatingProfit)
        );
        setField(
                statement,
                "operatingCashFlow",
                decimal(operatingCashFlow)
        );
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
