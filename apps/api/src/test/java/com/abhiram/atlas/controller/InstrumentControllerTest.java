package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.InstrumentResponse;
import com.abhiram.atlas.exception.GlobalExceptionHandler;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.service.InstrumentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for the instrument API contract.
 *
 * WHY SECURITY IS EXCLUDED HERE
 *
 * This test verifies routing, serialization and error handling. It
 * does not verify authorisation, and mixing the two would mean every
 * assertion about JSON shape depended on the security configuration
 * being correct.
 *
 * Three exclusions are needed because Spring Boot 4 splits security
 * autoconfiguration across several classes, and OAuth2 client
 * configuration requires an HttpSecurity bean that only exists when
 * the servlet security autoconfiguration is active. Excluding one
 * without the others leaves a half-configured context that fails in
 * a confusing way.
 *
 * The component scan filter additionally keeps the application's own
 * security package out of the context, so JwtAuthenticationFilter
 * and its dependencies are never constructed.
 *
 * Authorisation rules belong in their own test against the real
 * filter chain.
 */
@WebMvcTest(
        controllers = InstrumentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                ServletWebSecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.abhiram\\.atlas\\.security\\..*"
        )
)
@Import(GlobalExceptionHandler.class)
class InstrumentControllerTest {

    private static final UUID INSTRUMENT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstrumentService instrumentService;

    @Test
    @DisplayName("Returns all instruments")
    void returnsAllInstruments() throws Exception {

        when(instrumentService.getAll())
                .thenReturn(List.of(
                        new InstrumentResponse(
                                INSTRUMENT_ID,
                                "RELIANCE",
                                "Reliance Industries Ltd",
                                "NSE",
                                true
                        )
                ));

        mockMvc.perform(get("/api/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol")
                        .value("RELIANCE"))
                .andExpect(jsonPath("$[0].companyName")
                        .value("Reliance Industries Ltd"))
                .andExpect(jsonPath("$[0].exchange")
                        .value("NSE"))
                .andExpect(jsonPath("$[0].active")
                        .value(true));
    }

    @Test
    @DisplayName("Returns a single instrument by identifier")
    void returnsInstrumentById() throws Exception {

        when(instrumentService.getById(INSTRUMENT_ID))
                .thenReturn(new InstrumentResponse(
                        INSTRUMENT_ID,
                        "RELIANCE",
                        "Reliance Industries Ltd",
                        "NSE",
                        true
                ));

        mockMvc.perform(
                        get("/api/instruments/{id}",
                                INSTRUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(INSTRUMENT_ID.toString()))
                .andExpect(jsonPath("$.symbol")
                        .value("RELIANCE"));
    }

    @Test
    @DisplayName("Returns a single instrument by symbol")
    void returnsInstrumentBySymbol() throws Exception {

        when(instrumentService.getBySymbol("RELIANCE"))
                .thenReturn(new InstrumentResponse(
                        INSTRUMENT_ID,
                        "RELIANCE",
                        "Reliance Industries Ltd",
                        "NSE",
                        true
                ));

        mockMvc.perform(
                        get("/api/instruments/symbol/{symbol}",
                                "RELIANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol")
                        .value("RELIANCE"));
    }

    @Test
    @DisplayName("Returns 404 with a structured body for unknown symbols")
    void returnsNotFoundForUnknownSymbol() throws Exception {

        when(instrumentService.getBySymbol("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException(
                        "Instrument not found: UNKNOWN"));

        mockMvc.perform(
                        get("/api/instruments/symbol/{symbol}",
                                "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Instrument not found: UNKNOWN"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Returns 404 for an unknown identifier")
    void returnsNotFoundForUnknownId() throws Exception {

        UUID missingId = UUID.randomUUID();

        when(instrumentService.getById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Instrument not found: " + missingId));

        mockMvc.perform(
                        get("/api/instruments/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Returns JSON content type")
    void returnsJsonContentType() throws Exception {

        when(instrumentService.getAll())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/instruments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON));
    }
}
