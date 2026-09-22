package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ScoringProfile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for scoring profiles and sector relative comparison.
 *
 * These encode the decision that produced this feature: banks are
 * refused rather than scored badly.
 */
class SectorNormalizationServiceTest {

    private final SectorNormalizationService service =
            new SectorNormalizationService(null, null);

    @Test
    @DisplayName("Only operating companies are scoreable")
    void onlyOperatingCompaniesAreScoreable() {

        assertThat(ScoringProfile.OPERATING_COMPANY.isScoreable())
                .isTrue();

        // Three large banks previously scored an identical 10 out
        // of 100. Refusing to score is the correct response.
        assertThat(ScoringProfile.BANK.isScoreable())
                .isFalse();

        assertThat(ScoringProfile.FINANCIAL_SERVICES.isScoreable())
                .isFalse();
    }

    @Test
    @DisplayName("Unclassified companies default to unscoreable")
    void unknownDefaultsToUnscoreable() {

        // A permissive default would silently produce misleading
        // scores for any newly added company whose statements do
        // not follow the usual shape.
        assertThat(ScoringProfile.UNKNOWN.isScoreable()).isFalse();

        assertThat(ScoringProfile.parse(null))
                .isEqualTo(ScoringProfile.UNKNOWN);

        assertThat(ScoringProfile.parse(""))
                .isEqualTo(ScoringProfile.UNKNOWN);

        assertThat(ScoringProfile.parse("NOT_A_PROFILE"))
                .isEqualTo(ScoringProfile.UNKNOWN);
    }

    @Test
    @DisplayName("Parses known profiles case insensitively")
    void parsesKnownProfiles() {

        assertThat(ScoringProfile.parse("bank"))
                .isEqualTo(ScoringProfile.BANK);

        assertThat(ScoringProfile.parse("  OPERATING_COMPANY  "))
                .isEqualTo(ScoringProfile.OPERATING_COMPANY);
    }

    @Test
    @DisplayName("Expresses a margin above the sector median")
    void expressesMarginAboveSectorMedian() {

        // A 25 percent margin against a 20 percent sector median is
        // 25 percent higher, not 5 points higher.
        BigDecimal relative = service.relativeToSector(
                new BigDecimal("25.00"),
                new BigDecimal("20.00")
        );

        assertThat(relative).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("Expresses a margin below the sector median")
    void expressesMarginBelowSectorMedian() {

        BigDecimal relative = service.relativeToSector(
                new BigDecimal("15.00"),
                new BigDecimal("20.00")
        );

        assertThat(relative).isEqualByComparingTo("-25.00");
    }

    @Test
    @DisplayName("Returns zero when a margin matches the median")
    void returnsZeroAtTheMedian() {

        BigDecimal relative = service.relativeToSector(
                new BigDecimal("20.00"),
                new BigDecimal("20.00")
        );

        assertThat(relative).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Uses the absolute median so sign is preserved")
    void usesAbsoluteMedian() {

        // A company at minus 5 against a sector median of minus 10
        // is performing better, so the relative value must be
        // positive. Dividing by a negative median without taking
        // the absolute value would invert the sign.
        BigDecimal relative = service.relativeToSector(
                new BigDecimal("-5.00"),
                new BigDecimal("-10.00")
        );

        assertThat(relative).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Declines to compare against a zero median")
    void declinesZeroMedian() {

        // Dividing by zero is undefined. Returning a default would
        // manufacture a comparison that cannot be made.
        assertThat(service.relativeToSector(
                new BigDecimal("15.00"),
                BigDecimal.ZERO
        )).isNull();
    }

    @Test
    @DisplayName("Declines to compare when a value is missing")
    void declinesMissingValues() {

        assertThat(service.relativeToSector(
                null,
                new BigDecimal("20.00")
        )).isNull();

        assertThat(service.relativeToSector(
                new BigDecimal("20.00"),
                null
        )).isNull();

        assertThat(service.relativeToSector(null, null))
                .isNull();
    }
}
