package com.abhiram.atlas.domain;

/**
 * Determines whether a company's financial metrics are interpretable
 * by the standard Atlas scoring model.
 *
 * This exists because of a concrete failure. When the universe
 * expanded to twenty companies, HDFCBANK, ICICIBANK and SBIN all
 * scored exactly 10 out of 100. Three very different lenders landing
 * on an identical score is not a finding, it is a metric that does
 * not apply.
 *
 * Banks have no revenue in the manufacturing sense, and their
 * operating cash flow reflects deposit and lending movements rather
 * than profitability. Computing a margin on those figures produces a
 * number that looks meaningful and is not.
 *
 * Refusing to score is more useful than scoring badly, because a
 * wrong number gets acted on while an explicit gap does not.
 */
public enum ScoringProfile {

    OPERATING_COMPANY(true),

    BANK(false),

    FINANCIAL_SERVICES(false),

    /**
     * Default for unclassified companies.
     *
     * Defaulting to unscoreable rather than to the standard profile
     * means a newly added company must be classified deliberately. A
     * permissive default would silently produce misleading scores for
     * any company whose statements do not follow the usual shape.
     */
    UNKNOWN(false);

    private final boolean scoreable;

    ScoringProfile(boolean scoreable) {
        this.scoreable = scoreable;
    }

    public boolean isScoreable() {
        return scoreable;
    }

    public static ScoringProfile parse(String value) {

        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return ScoringProfile.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
