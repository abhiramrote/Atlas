package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.RiskMetricsResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for backward looking risk metrics.
 */
@ExtendWith(MockitoExtension.class)
class RiskMetricsServiceTest {

    private static final UUID INSTRUMENT_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock
    private PriceBarRepository priceBarRepository;

    @InjectMocks
    private RiskMetricsService service;

    private Instrument instrument;

    @BeforeEach
    void setUp() throws Exception {
        instrument = newInstrument();
    }

    @Test
    @DisplayName("Reports zero drawdown for a monotonically rising series")
    void reportsZeroDrawdownWhenPriceOnlyRises() throws Exception {

        stubBars(
                "100.00",
                "102.00",
                "104.00",
                "106.00",
                "108.00",
                "110.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.maximumDrawdownPercent())
                .isEqualByComparingTo("0.00");

        assertThat(response.peakClose())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Calculates a known peak to trough decline")
    void calculatesKnownDrawdown() throws Exception {

        // Peak 130, trough 91.
        // (91 - 130) / 130 * 100 = -30 percent.
        stubBars(
                "100.00",
                "115.00",
                "130.00",
                "110.00",
                "100.00",
                "91.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.maximumDrawdownPercent())
                .isEqualByComparingTo("-30.00");

        assertThat(response.peakClose())
                .isEqualByComparingTo("130.00");

        assertThat(response.troughClose())
                .isEqualByComparingTo("91.00");
    }

    @Test
    @DisplayName("Measures drawdown against the earlier running peak")
    void measuresDrawdownAgainstEarlierPeak() throws Exception {

        // A later recovery above the trough must not erase the
        // recorded worst decline.
        stubBars(
                "100.00",
                "120.00",
                "90.00",
                "125.00",
                "120.00",
                "118.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        // (90 - 120) / 120 * 100 = -25 percent.
        assertThat(response.maximumDrawdownPercent())
                .isEqualByComparingTo("-25.00");
    }

    @Test
    @DisplayName("Reports near zero volatility for a flat series")
    void reportsNearZeroVolatilityForFlatSeries() throws Exception {

        stubBars(
                "100.00",
                "100.00",
                "100.00",
                "100.00",
                "100.00",
                "100.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.annualisedVolatilityPercent())
                .isEqualByComparingTo("0.00");

        assertThat(response.riskBand()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Reports higher volatility for a more erratic series")
    void reportsHigherVolatilityForErraticSeries() throws Exception {

        stubBars(
                "100.00",
                "100.20",
                "100.10",
                "100.30",
                "100.25",
                "100.40"
        );

        RiskMetricsResponse calmResponse =
                service.getRiskMetrics(INSTRUMENT_ID);

        BigDecimal calmVolatility =
                calmResponse.annualisedVolatilityPercent();

        stubBars(
                "100.00",
                "112.00",
                "95.00",
                "118.00",
                "92.00",
                "121.00"
        );

        RiskMetricsResponse erraticResponse =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(erraticResponse.annualisedVolatilityPercent())
                .isGreaterThan(calmVolatility);

        assertThat(erraticResponse.riskBand()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Counts only usable observations")
    void countsUsableObservations() throws Exception {

        stubBars(
                "100.00",
                "101.00",
                "102.00",
                "103.00",
                "104.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.observationDays()).isEqualTo(5);
        assertThat(response.symbol()).isEqualTo("RELIANCE");
        assertThat(response.instrumentId()).isEqualTo(INSTRUMENT_ID);
    }

    @Test
    @DisplayName("Warns that a short window is indicative only")
    void warnsAboutShortWindow() throws Exception {

        stubBars(
                "100.00",
                "101.00",
                "102.00",
                "103.00",
                "104.00"
        );

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.note())
                .containsIgnoringCase("indicative")
                .containsIgnoringCase("splits");
    }

    @Test
    @DisplayName("Ignores bars without a usable close price")
    void ignoresBarsWithoutClosePrice() throws Exception {

        List<PriceBar> bars = new ArrayList<>();

        bars.add(newBar("2026-09-10", "100.00"));
        bars.add(newBar("2026-09-11", null));
        bars.add(newBar("2026-09-12", "102.00"));
        bars.add(newBar("2026-09-13", "103.00"));
        bars.add(newBar("2026-09-14", "104.00"));
        bars.add(newBar("2026-09-15", "105.00"));

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(bars);

        RiskMetricsResponse response =
                service.getRiskMetrics(INSTRUMENT_ID);

        assertThat(response.observationDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("Fails when too few price bars are stored")
    void failsWithInsufficientHistory() throws Exception {

        stubBars("100.00", "101.00", "102.00");

        assertThatThrownBy(() ->
                service.getRiskMetrics(INSTRUMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price bars are required");
    }

    @Test
    @DisplayName("Fails when no price history is stored")
    void failsWithNoHistory() {

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.getRiskMetrics(INSTRUMENT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----------------------------------------------------------
    // Fixture helpers
    // ----------------------------------------------------------

    /**
     * Accepts closes oldest first, then stubs the repository to
     * return them newest first, matching production behaviour.
     */
    private void stubBars(String... closesOldestFirst)
            throws Exception {

        List<PriceBar> bars = new ArrayList<>();

        LocalDate date = LocalDate.of(2026, 9, 1);

        for (String close : closesOldestFirst) {
            bars.add(newBar(date.toString(), close));
            date = date.plusDays(1);
        }

        List<PriceBar> newestFirst = new ArrayList<>(bars);
        java.util.Collections.reverse(newestFirst);

        when(priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(INSTRUMENT_ID))
                .thenReturn(newestFirst);
    }

    private Instrument newInstrument() throws Exception {

        Instrument newInstrument = newInstance(Instrument.class);

        setField(newInstrument, "id", INSTRUMENT_ID);
        setField(newInstrument, "symbol", "RELIANCE");
        setField(
                newInstrument,
                "companyName",
                "Reliance Industries Ltd"
        );
        setField(newInstrument, "exchange", "NSE");
        setField(newInstrument, "active", Boolean.TRUE);
        setField(newInstrument, "createdAt", LocalDateTime.now());
        setField(newInstrument, "updatedAt", LocalDateTime.now());

        return newInstrument;
    }

    private PriceBar newBar(
            String tradeDate,
            String closePrice
    ) throws Exception {

        PriceBar bar = newInstance(PriceBar.class);

        BigDecimal close = closePrice == null
                ? null
                : new BigDecimal(closePrice);

        setField(bar, "id", UUID.randomUUID());
        setField(bar, "instrument", instrument);
        setField(bar, "tradeDate", LocalDate.parse(tradeDate));
        setField(bar, "openPrice", close);
        setField(bar, "highPrice", close);
        setField(bar, "lowPrice", close);
        setField(bar, "closePrice", close);
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
