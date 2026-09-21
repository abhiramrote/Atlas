package com.abhiram.atlas.controller;

import com.abhiram.atlas.dto.InstrumentResponse;
import com.abhiram.atlas.exception.GlobalExceptionHandler;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.service.InstrumentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for the instrument API contract.
 *
 * The service layer is mocked so these tests verify only
 * routing, serialization and error handling.
 *
 * NOTE ON SPRING BOOT VERSION
 * ---------------------------
 * This project uses Spring Boot 4, where @MockBean is removed.
 * Use org.springframework.test.context.bean.override.mockito.MockitoBean
 * as shown below. If your IDE cannot resolve MockitoBean, confirm the
 * spring-boot-starter-webmvc-test dependency is present.
 *
 * Also note: @WebMvcTest is imported in Spring Boot 4 from
 * org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
 * If that import fails, use the fully qualified name your
 * Spring Boot version provides.
 */
@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(
        controllers = InstrumentController.class
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
                .andExpect(
                        jsonPath("$[0].symbol")
                                .value("RELIANCE"))
                .andExpect(
                        jsonPath("$[0].companyName")
                                .value("Reliance Industries Ltd"))
                .andExpect(
                        jsonPath("$[0].exchange")
                                .value("NSE"))
                .andExpect(
                        jsonPath("$[0].active")
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
                .andExpect(
                        jsonPath("$.id")
                                .value(INSTRUMENT_ID.toString()))
                .andExpect(
                        jsonPath("$.symbol")
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
                .andExpect(
                        jsonPath("$.symbol")
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
                .andExpect(
                        jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.message")
                                .value("Instrument not found: UNKNOWN"))
                .andExpect(
                        jsonPath("$.timestamp").exists());
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
                .andExpect(
                        jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Returns JSON content type")
    void returnsJsonContentType() throws Exception {

        when(instrumentService.getAll())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/instruments"))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result
                                .MockMvcResultMatchers
                                .content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON));
    }
}
