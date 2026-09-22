package com.abhiram.atlas.domain;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Plausible net margin ceilings by sector.
 *
 * WHY A FLAT THRESHOLD FAILED
 *
 * A single 40 percent ceiling correctly caught HINDALCO, whose
 * corrupted FY2025 reported a 41.66 percent margin for an aluminium
 * business. It also wrongly flagged POWERGRID at 44.53 percent, which
 * is entirely genuine. Regulated power transmission earns those
 * margins because it is a monopoly with guaranteed returns.
 *
 * The same lesson the scoring engine learned with banks: structural
 * differences between industries cannot be handled by one number.
 *
 * HOW THESE BANDS WERE CHOSEN
 *
 * Each ceiling sits above what a strong company in that sector can
 * plausibly achieve, not at the sector average. The purpose is to
 * catch figures that are not really net income, not to flag
 * exceptional performance.
 *
 * Deliberately generous. A check that fires on genuine excellence
 * trains you to ignore it, which is worse than having no check.
 */
public final class SectorMarginBand {

    /**
     * Applied when a sector has no specific band.
     *
     * Kept at 40 because that is the level at which a figure stops
     * looking like net income for a typical operating company.
     */
    private static final BigDecimal DEFAULT_CEILING =
            BigDecimal.valueOf(40);

    private static final Map<String, BigDecimal> CEILINGS =
            Map.ofEntries(

                    // Regulated monopoly with guaranteed returns.
                    // POWERGRID genuinely reports mid forties.
                    Map.entry("Power", BigDecimal.valueOf(60)),

                    // Commodity processing. Margins are structurally
                    // thin, so anything above the mid twenties is
                    // almost certainly not net income.
                    Map.entry(
                            "Metals and Mining",
                            BigDecimal.valueOf(25)),

                    // Refining and marketing run thin. Exploration
                    // runs richer, which is why this sector really
                    // ought to be split.
                    Map.entry("Energy", BigDecimal.valueOf(30)),

                    // Spectrum and network economics allow high
                    // margins in good years.
                    Map.entry(
                            "Telecommunications",
                            BigDecimal.valueOf(40)),

                    Map.entry(
                            "Information Technology",
                            BigDecimal.valueOf(35)),

                    Map.entry(
                            "Consumer Goods",
                            BigDecimal.valueOf(35)),

                    Map.entry("Healthcare", BigDecimal.valueOf(35)),

                    Map.entry("Automobile", BigDecimal.valueOf(35)),

                    Map.entry(
                            "Construction Materials",
                            BigDecimal.valueOf(35)),

                    Map.entry(
                            "Construction",
                            BigDecimal.valueOf(25))
            );

    private SectorMarginBand() {
    }

    public static BigDecimal ceilingFor(String sector) {

        if (sector == null || sector.isBlank()) {
            return DEFAULT_CEILING;
        }

        return CEILINGS.getOrDefault(
                sector.trim(),
                DEFAULT_CEILING
        );
    }

    public static boolean hasSpecificBand(String sector) {
        return sector != null
                && CEILINGS.containsKey(sector.trim());
    }
}
