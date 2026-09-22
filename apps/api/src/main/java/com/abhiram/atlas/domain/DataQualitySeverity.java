package com.abhiram.atlas.domain;

/**
 * How much a data quality issue should affect trust in a score.
 *
 * The distinction matters because the response differs. A warning
 * says look closely. An error says do not rely on this number.
 */
public enum DataQualitySeverity {

    /**
     * Unusual but possible. Worth inspecting before acting.
     */
    WARNING,

    /**
     * Almost certainly a data problem rather than a business event.
     * Any score derived from the affected period should be treated
     * as unreliable.
     */
    ERROR
}