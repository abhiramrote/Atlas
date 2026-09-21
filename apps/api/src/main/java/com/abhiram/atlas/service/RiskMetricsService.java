package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.RiskMetricsResponse;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.repository.PriceBarRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Computes backward looking risk metrics from stored price bars.
 *
 * Deliberate scope limits:
 *
 * - Only daily close prices are used. Intraday range is ignored.
 * - Returns are simple percentage changes, not log returns. Simple
 *   returns are easier to reconcile against the price history shown
 *   in the user interface.
 * - No dividend or corporate action adjustment is applied, because
 *   Atlas does not yet store corporate actions. A split would appear
 *   as an extreme return and distort both metrics. This limitation
 *   is reported in the response note.
 * - Volatility is annualised using 252 trading days, a common market
 *   convention. With roughly thirty observations the estimate is
 *   indicative only.
 */
@Service
@Transactional(readOnly = true)
public class RiskMetricsService {

    private static final int MINIMUM_OBSERVATIONS = 5;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final MathContext CONTEXT =
            new MathContext(12, RoundingMode.HALF_UP);

    private final PriceBarRepository priceBarRepository;

    public RiskMetricsService(
            PriceBarRepository priceBarRepository
    ) {
        this.priceBarRepository = priceBarRepository;
    }

    public RiskMetricsResponse getRiskMetrics(UUID instrumentId) {

        List<PriceBar> bars = loadAscendingBars(instrumentId);

        if (bars.size() < MINIMUM_OBSERVATIONS) {
            throw new IllegalArgumentException(
                    "At least " + MINIMUM_OBSERVATIONS
                            + " price bars are required to compute "
                            + "risk metrics"
            );
        }

        List<BigDecimal> closes = bars.stream()
                .map(PriceBar::getClosePrice)
                .toList();

        BigDecimal volatility = annualisedVolatility(closes);
        Drawdown drawdown = maximumDrawdown(closes);

        return new RiskMetricsResponse(
                instrumentId,
                bars.get(0).getInstrument().getSymbol(),
                bars.size(),
                volatility,
                drawdown.percent(),
                drawdown.peak(),
                drawdown.trough(),
                classifyRisk(volatility, drawdown.percent()),
                buildNote(bars.size())
        );
    }

    /**
     * Returns bars oldest first with usable close prices only.
     */
    private List<PriceBar> loadAscendingBars(UUID instrumentId) {

        return priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(instrumentId)
                .stream()
                .filter(bar -> bar.getClosePrice() != null)
                .filter(bar ->
                        bar.getClosePrice()
                                .compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PriceBar::getTradeDate))
                .toList();
    }

    /**
     * Standard deviation of daily simple returns, scaled to a year.
     *
     * The sample standard deviation uses n-1 in the denominator
     * because the stored history is a sample of the return process,
     * not the complete population.
     */
    private BigDecimal annualisedVolatility(
            List<BigDecimal> closes
    ) {
        List<BigDecimal> returns = new ArrayList<>();

        for (int index = 1; index < closes.size(); index++) {

            BigDecimal previous = closes.get(index - 1);
            BigDecimal current = closes.get(index);

            BigDecimal dailyReturn = current
                    .subtract(previous)
                    .divide(previous, CONTEXT);

            returns.add(dailyReturn);
        }

        if (returns.size() < 2) {
            return null;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(returns.size()),
                        CONTEXT
                );

        BigDecimal sumSquaredDeviations = BigDecimal.ZERO;

        for (BigDecimal value : returns) {
            BigDecimal deviation = value.subtract(mean);

            sumSquaredDeviations = sumSquaredDeviations.add(
                    deviation.multiply(deviation, CONTEXT)
            );
        }

        BigDecimal variance = sumSquaredDeviations.divide(
                BigDecimal.valueOf(returns.size() - 1L),
                CONTEXT
        );

        double dailyStandardDeviation =
                Math.sqrt(variance.doubleValue());

        double annualised = dailyStandardDeviation
                * Math.sqrt(TRADING_DAYS_PER_YEAR);

        return BigDecimal.valueOf(annualised)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Largest peak to trough decline within the stored window.
     *
     * The running peak is updated as the series advances, so the
     * trough is always evaluated against a peak that occurred
     * earlier in time.
     */
    private Drawdown maximumDrawdown(List<BigDecimal> closes) {

        BigDecimal runningPeak = closes.get(0);

        BigDecimal worstDrawdown = BigDecimal.ZERO;
        BigDecimal worstPeak = runningPeak;
        BigDecimal worstTrough = runningPeak;

        for (BigDecimal close : closes) {

            if (close.compareTo(runningPeak) > 0) {
                runningPeak = close;
            }

            BigDecimal decline = close
                    .subtract(runningPeak)
                    .divide(runningPeak, CONTEXT)
                    .multiply(ONE_HUNDRED);

            if (decline.compareTo(worstDrawdown) < 0) {
                worstDrawdown = decline;
                worstPeak = runningPeak;
                worstTrough = close;
            }
        }

        return new Drawdown(
                worstDrawdown.setScale(2, RoundingMode.HALF_UP),
                worstPeak,
                worstTrough
        );
    }

    /**
     * Combines volatility and drawdown into a coarse band.
     *
     * The worse of the two signals determines the band, because a
     * company can look calm on volatility while still having suffered
     * a severe single decline.
     */
    private String classifyRisk(
            BigDecimal volatilityPercent,
            BigDecimal drawdownPercent
    ) {
        if (volatilityPercent == null) {
            return "UNKNOWN";
        }

        double volatility = volatilityPercent.doubleValue();
        double drawdown = Math.abs(drawdownPercent.doubleValue());

        if (volatility >= 45 || drawdown >= 25) {
            return "HIGH";
        }

        if (volatility >= 25 || drawdown >= 15) {
            return "ELEVATED";
        }

        if (volatility >= 15 || drawdown >= 8) {
            return "MODERATE";
        }

        return "LOW";
    }

    private String buildNote(int observationDays) {

        StringBuilder note = new StringBuilder();

        note.append("Computed from ")
                .append(observationDays)
                .append(" stored daily closes. ");

        if (observationDays < 60) {
            note.append(
                    "This window is short, so the volatility "
                            + "estimate is indicative only. ");
        }

        note.append(
                "Prices are not adjusted for splits or dividends, "
                        + "so corporate actions may distort these "
                        + "metrics.");

        return note.toString();
    }

    private record Drawdown(
            BigDecimal percent,
            BigDecimal peak,
            BigDecimal trough
    ) {
    }
}
