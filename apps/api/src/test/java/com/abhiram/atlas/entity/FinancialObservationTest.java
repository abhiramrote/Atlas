package com.abhiram.atlas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for point-in-time observation semantics.
 *
 * The knowledge date rules encoded here are what separate an honest
 * backtest from one that silently uses future information.
 */
class FinancialObservationTest {

    @Test
    @DisplayName("Is knowable on or after its knowledge date")
    void knowableOnOrAfterKnowledgeDate() {

        FinancialObservation observation = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        assertThat(observation.wasKnownOn(
                LocalDate.of(2024, 5, 15))).isTrue();

        assertThat(observation.wasKnownOn(
                LocalDate.of(2024, 12, 31))).isTrue();
    }

    @Test
    @DisplayName("Is not knowable before its knowledge date")
    void notKnowableBeforeKnowledgeDate() {

        // FY2024 ends 31 March 2024 but results are published in May.
        // A backtest scoring this company on 1 April 2024 must not
        // see these figures.
        FinancialObservation observation = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        assertThat(observation.wasKnownOn(
                LocalDate.of(2024, 4, 1))).isFalse();

        assertThat(observation.wasKnownOn(
                LocalDate.of(2024, 5, 14))).isFalse();
    }

    @Test
    @DisplayName("Requires a knowledge date")
    void requiresKnowledgeDate() {

        assertThatThrownBy(() ->
                FinancialObservation.record(
                        UUID.randomUUID(),
                        null,
                        2024,
                        4,
                        LocalDate.of(2024, 3, 31),
                        null,
                        new BigDecimal("100.00"),
                        null, null, null,
                        "TEST", "CRORE", "INR", 1
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Knowledge date is required");
    }

    @Test
    @DisplayName("Records a supersession without altering figures")
    void supersedeDoesNotAlterFigures() {

        FinancialObservation original = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        UUID replacementId = UUID.randomUUID();

        original.supersede(replacementId);

        assertThat(original.isSuperseded()).isTrue();
        assertThat(original.getSupersededBy())
                .isEqualTo(replacementId);
        assertThat(original.getSupersededAt()).isNotNull();

        // The original figure survives. A restatement is new
        // information, not a correction to what was known.
        assertThat(original.getRevenue())
                .isEqualByComparingTo("153670.00");

        assertThat(original.getKnowledgeDate())
                .isEqualTo(LocalDate.of(2024, 5, 15));
    }

    @Test
    @DisplayName("Refuses to supersede twice")
    void refusesDoubleSupersede() {

        FinancialObservation original = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        original.supersede(UUID.randomUUID());

        assertThatThrownBy(() ->
                original.supersede(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already superseded");
    }

    @Test
    @DisplayName("Keeps reporting lag visible")
    void keepsReportingLagVisible() {

        FinancialObservation observation = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        // Period ended 31 March, learned 15 May. That 45 day gap is
        // the reporting lag, and it must remain inspectable.
        assertThat(observation.getPeriodEndDate())
                .isEqualTo(LocalDate.of(2024, 3, 31));

        assertThat(observation.getKnowledgeDate())
                .isAfter(observation.getPeriodEndDate());
    }

    @Test
    @DisplayName("Carries unit and currency")
    void carriesUnitAndCurrency() {

        FinancialObservation observation = observation(
                2024,
                LocalDate.of(2024, 5, 15),
                "153670.00"
        );

        // IndianAPI reports in crores. Mixing units across providers
        // without conversion would corrupt absolute comparisons.
        assertThat(observation.getUnit()).isEqualTo("CRORE");
        assertThat(observation.getCurrency()).isEqualTo("INR");
    }

    private FinancialObservation observation(
            int fiscalYear,
            LocalDate knowledgeDate,
            String revenue
    ) {
        return FinancialObservation.record(
                UUID.randomUUID(),
                null,
                fiscalYear,
                4,
                LocalDate.of(fiscalYear, 3, 31),
                knowledgeDate,
                new BigDecimal(revenue),
                new BigDecimal("26233.00"),
                new BigDecimal("31747.00"),
                new BigDecimal("25210.00"),
                "INDIAN_API",
                "CRORE",
                "INR",
                1
        );
    }
}
