package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.dto.TechnicalScoreResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;

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
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for combined scoring and partial availability behaviour.
 */
@ExtendWith(MockitoExtension.class)
class OpportunityScoreV2ServiceTest {

    private static final UUID COMPANY_ID =
            UUID.fromString("650e8400-e29b-41d4-a716-446655440002");

    private static final UUID INSTRUMENT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @Mock
    private OpportunityScoreService opportunityService;

    @Mock
    private TechnicalScoreService technicalService;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private OpportunityScoreV2Service service;

    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        Instrument instrument = newInstrument(
                INSTRUMENT_ID,
                "TCS",
                "Tata Consultancy Services Ltd"
        );

        company = newCompany(COMPANY_ID, instrument);
    }

    @Test
    @DisplayName("Combines both components when price history exists")
    void combinesBothComponentsWhenAvailable() {

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(opportunityService.getScore(COMPANY_ID))
                .thenReturn(91);

        when(technicalService.score(INSTRUMENT_ID))
                .thenReturn(new TechnicalScoreResponse(
                        "TCS",
                        new BigDecimal("1.51"),
                        5
                ));

        OpportunityScoreV2Response response =
                service.score(COMPANY_ID);

        assertThat(response.fundamentalScore()).isEqualTo(91);
        assertThat(response.technicalScore()).isEqualTo(5);
        assertThat(response.finalScore()).isEqualTo(96);
        assertThat(response.maximumScore()).isEqualTo(110);

        assertThat(response.technicalScoreAvailable()).isTrue();
        assertThat(response.technicalScoreNote()).isNull();
    }

    @Test
    @DisplayName("Keeps the fundamental score when price history is missing")
    void degradesGracefullyWhenPriceHistoryMissing() {

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(opportunityService.getScore(COMPANY_ID))
                .thenReturn(74);

        when(technicalService.score(INSTRUMENT_ID))
                .thenThrow(new IllegalArgumentException(
                        "At least 2 price bars required"));

        OpportunityScoreV2Response response =
                service.score(COMPANY_ID);

        assertThat(response.fundamentalScore()).isEqualTo(74);

        // Null, not zero. Zero would imply a measured flat momentum.
        assertThat(response.technicalScore()).isNull();

        assertThat(response.finalScore()).isEqualTo(74);
        assertThat(response.maximumScore()).isEqualTo(100);

        assertThat(response.technicalScoreAvailable()).isFalse();
        assertThat(response.technicalScoreNote())
                .containsIgnoringCase("price history");
    }

    @Test
    @DisplayName("Rates partial results against the achievable maximum")
    void ratesPartialResultsFairly() {

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(opportunityService.getScore(COMPANY_ID))
                .thenReturn(91);

        when(technicalService.score(INSTRUMENT_ID))
                .thenThrow(new IllegalArgumentException(
                        "At least 2 price bars required"));

        OpportunityScoreV2Response response =
                service.score(COMPANY_ID);

        // 91 out of an achievable 100 is 91 percent, so the company
        // is still rated ELITE despite the missing technical component.
        assertThat(response.finalScore()).isEqualTo(91);
        assertThat(response.rating()).isEqualTo("ELITE");
    }

    @Test
    @DisplayName("Propagates failure when the fundamental score is missing")
    void propagatesFundamentalFailure() {

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        when(opportunityService.getScore(COMPANY_ID))
                .thenThrow(new ResourceNotFoundException(
                        "At least two financial periods are required"));

        assertThatThrownBy(() -> service.score(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("financial periods");
    }

    @Test
    @DisplayName("Fails when the company does not exist")
    void failsWhenCompanyMissing() {

        when(companyRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.score(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found");
    }

        @Test
    @DisplayName("Assigns ratings across the percentage bands")
    void assignsRatingsAcrossBands() {

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        // doThrow avoids invoking the mock during stubbing,
        // which is required when re-stubbing inside a loop.
        doThrow(new IllegalArgumentException(
                "At least 2 price bars required"))
                .when(technicalService)
                .score(INSTRUMENT_ID);

        assertRating(95, "ELITE");
        assertRating(83, "STRONG");
        assertRating(70, "GOOD");
        assertRating(55, "MODERATE");
        assertRating(30, "WEAK");
    }

    private void assertRating(
            int fundamentalScore,
            String expectedRating
    ) {
        doReturn(fundamentalScore)
                .when(opportunityService)
                .getScore(COMPANY_ID);

        OpportunityScoreV2Response response =
                service.score(COMPANY_ID);

        assertThat(response.rating())
                .as("score %d", fundamentalScore)
                .isEqualTo(expectedRating);
    }

    // ----------------------------------------------------------
    // Fixture helpers
    // ----------------------------------------------------------

    private Instrument newInstrument(
            UUID id,
            String symbol,
            String companyName
    ) throws Exception {

        Instrument instrument = newInstance(Instrument.class);

        setField(instrument, "id", id);
        setField(instrument, "symbol", symbol);
        setField(instrument, "companyName", companyName);
        setField(instrument, "exchange", "NSE");
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
        setField(newCompany, "sector", "Information Technology");
        setField(newCompany, "industry", "IT Services");
        setField(newCompany, "website", "https://example.com");
        setField(newCompany, "description", "Test fixture");
        setField(newCompany, "createdAt", LocalDateTime.now());
        setField(newCompany, "updatedAt", LocalDateTime.now());

        return newCompany;
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
