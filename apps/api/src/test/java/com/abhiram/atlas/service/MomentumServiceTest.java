package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.MomentumResponse;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.repository.PriceBarRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the momentum calculation.
 *
 * Momentum compares the newest stored close price with the
 * oldest stored close price for an instrument.
 */
@ExtendWith(MockitoExtension.class)
class MomentumServiceTest {

    private static final UUID INSTRUMENT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock
    private PriceBarRepository priceBarRepository;

    @InjectMocks
    private MomentumService service;

    private Instrument instrument;

    @BeforeEach
    void setUp() throws Exception {
        instrument = newInstrument("RELIANCE");
    }

    @Test
    @DisplayName("Calculates a positive momentum percentage")
    void calculatesPositiveMomentum() throws Exception {

        // Repository returns newest first.
        // Oldest close 2980, newest close 3025.
        // (3025 - 2980) / 2980 * 100 = 1.51 percent.
        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of(
                        newBar("2026-09-19", "3025.00"),
                        newBar("2026-09-18", "3015.00"),
                        newBar("2026-09-17", "3005.00"),
                        newBar("2026-09-16", "2995.00"),
                        newBar("2026-09-15", "2980.00")
                ));

        MomentumResponse response =
                service.getMomentum(INSTRUMENT_ID);

        assertThat(response.symbol()).isEqualTo("RELIANCE");

        assertThat(response.firstClose())
                .isEqualByComparingTo("2980.00");

        assertThat(response.latestClose())
                .isEqualByComparingTo("3025.00");

        assertThat(response.priceChangePercent())
                .isEqualByComparingTo("1.51");

        assertThat(response.trend()).isEqualTo("UPTREND");
    }

    @Test
    @DisplayName("Calculates a negative momentum percentage")
    void calculatesNegativeMomentum() throws Exception {

        // Oldest close 12.53, newest close 10.82.
        // (10.82 - 12.53) / 12.53 * 100 = -13.65 percent.
        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of(
                        newBar("2026-09-18", "10.82"),
                        newBar("2026-09-17", "11.07"),
                        newBar("2026-08-07", "12.53")
                ));

        MomentumResponse response =
                service.getMomentum(INSTRUMENT_ID);

        assertThat(response.priceChangePercent())
                .isEqualByComparingTo("-13.65");

        assertThat(response.trend()).isEqualTo("DOWNTREND");
    }

    @Test
    @DisplayName("Treats a flat price series as a downtrend boundary")
    void treatsFlatSeriesAsNonPositive() throws Exception {

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of(
                        newBar("2026-09-19", "3000.00"),
                        newBar("2026-09-18", "3000.00")
                ));

        MomentumResponse response =
                service.getMomentum(INSTRUMENT_ID);

        assertThat(response.priceChangePercent())
                .isEqualByComparingTo("0.00");

        // The current rule classifies non-positive change
        // as DOWNTREND. This test documents that behaviour
        // so any future change is intentional.
        assertThat(response.trend()).isEqualTo("DOWNTREND");
    }

    @Test
    @DisplayName("Fails when fewer than two price bars are stored")
    void failsWithInsufficientPriceHistory() throws Exception {

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of(
                        newBar("2026-09-19", "3025.00")
                ));

        assertThatThrownBy(() ->
                service.getMomentum(INSTRUMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 price bars");
    }

    @Test
    @DisplayName("Fails when no price history exists")
    void failsWithNoPriceHistory() {

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.getMomentum(INSTRUMENT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----------------------------------------------------------
    // Test fixture helpers
    // ----------------------------------------------------------

    private Instrument newInstrument(String symbol)
            throws Exception {

        Instrument newInstrument =
                newInstance(Instrument.class);

        setField(newInstrument, "id", INSTRUMENT_ID);
        setField(newInstrument, "symbol", symbol);
        setField(
                newInstrument,
                "companyName",
                "Test Company Ltd"
        );
        setField(newInstrument, "exchange", "NSE");
        setField(newInstrument, "active", Boolean.TRUE);
        setField(
                newInstrument,
                "createdAt",
                LocalDateTime.now()
        );
        setField(
                newInstrument,
                "updatedAt",
                LocalDateTime.now()
        );

        return newInstrument;
    }

    private PriceBar newBar(
            String tradeDate,
            String closePrice
    ) throws Exception {

        PriceBar bar = newInstance(PriceBar.class);

        setField(bar, "id", UUID.randomUUID());
        setField(bar, "instrument", instrument);
        setField(bar, "tradeDate", LocalDate.parse(tradeDate));
        setField(bar, "openPrice", new BigDecimal(closePrice));
        setField(bar, "highPrice", new BigDecimal(closePrice));
        setField(bar, "lowPrice", new BigDecimal(closePrice));
        setField(bar, "closePrice", new BigDecimal(closePrice));
        setField(bar, "volume", 1_000_000L);
        setField(bar, "createdAt", LocalDateTime.now());

        return bar;
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
