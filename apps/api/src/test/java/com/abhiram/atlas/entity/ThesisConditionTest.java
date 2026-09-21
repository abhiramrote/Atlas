package com.abhiram.atlas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for invalidation condition evaluation.
 */
class ThesisConditionTest {

    @Test
    @DisplayName("BELOW breaches when the observed value is lower")
    void belowBreachesWhenLower() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "17.00"
        );

        assertThat(condition.evaluate(new BigDecimal("16.50")))
                .isTrue();

        assertThat(condition.evaluate(new BigDecimal("17.00")))
                .isFalse();

        assertThat(condition.evaluate(new BigDecimal("20.60")))
                .isFalse();
    }

    @Test
    @DisplayName("ABOVE breaches when the observed value is higher")
    void aboveBreachesWhenHigher() {

        ThesisCondition condition = condition(
                "DEBT_TO_EQUITY",
                "ABOVE",
                "1.00"
        );

        assertThat(condition.evaluate(new BigDecimal("1.40")))
                .isTrue();

        assertThat(condition.evaluate(new BigDecimal("1.00")))
                .isFalse();

        assertThat(condition.evaluate(new BigDecimal("0.30")))
                .isFalse();
    }

    @Test
    @DisplayName("Inclusive comparisons breach at the threshold")
    void inclusiveComparisonsBreachAtBoundary() {

        ThesisCondition atOrBelow = condition(
                "REVENUE_GROWTH",
                "AT_OR_BELOW",
                "0.00"
        );

        assertThat(atOrBelow.evaluate(BigDecimal.ZERO)).isTrue();

        ThesisCondition atOrAbove = condition(
                "VOLATILITY",
                "AT_OR_ABOVE",
                "45.00"
        );

        assertThat(atOrAbove.evaluate(new BigDecimal("45.00")))
                .isTrue();
    }

    @Test
    @DisplayName("Missing data never counts as a breach")
    void missingDataDoesNotBreach() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "17.00"
        );

        // Absence of evidence is not evidence of failure. Treating
        // null as a breach would produce false invalidations every
        // time a provider call fails.
        assertThat(condition.evaluate(null)).isFalse();
    }

    @Test
    @DisplayName("A breach records the observed value")
    void breachRecordsObservedValue() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "17.00"
        );

        assertThat(condition.getBreached()).isFalse();

        condition.markBreached(new BigDecimal("15.20"));

        assertThat(condition.getBreached()).isTrue();
        assertThat(condition.getBreachedValue())
                .isEqualByComparingTo("15.20");
        assertThat(condition.getBreachedAt()).isNotNull();
    }

    @Test
    @DisplayName("The first breach value is never overwritten")
    void firstBreachIsPreserved() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "BELOW",
                "17.00"
        );

        condition.markBreached(new BigDecimal("15.20"));
        condition.markBreached(new BigDecimal("12.00"));

        // The moment the thesis first broke is the fact that
        // matters. Later deterioration must not rewrite it.
        assertThat(condition.getBreachedValue())
                .isEqualByComparingTo("15.20");
    }

    @Test
    @DisplayName("An unknown comparison is rejected")
    void unknownComparisonIsRejected() {

        ThesisCondition condition = condition(
                "OPERATING_MARGIN",
                "SIDEWAYS",
                "17.00"
        );

        assertThatThrownBy(() ->
                condition.evaluate(new BigDecimal("10.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported comparison");
    }

    private ThesisCondition condition(
            String metric,
            String comparison,
            String threshold
    ) {
        return ThesisCondition.create(
                UUID.randomUUID(),
                null,
                metric,
                comparison,
                new BigDecimal(threshold),
                "Test condition"
        );
    }
}
