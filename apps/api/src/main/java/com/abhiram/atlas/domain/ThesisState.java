package com.abhiram.atlas.domain;

import java.util.Set;

/**
 * Lifecycle states for a research thesis.
 *
 * The transitions are deliberately restrictive. A thesis cannot move
 * directly from DRAFT to CONFIRMED, because confirmation must follow
 * observation. An INVALIDATED thesis cannot be quietly returned to
 * CONFIRMED, because that is the exact behaviour the engine exists to
 * prevent: retroactively rewriting a failed judgement.
 */
public enum ThesisState {

    /**
     * Being written. Not yet committed and freely editable.
     */
    DRAFT,

    /**
     * Published and frozen. The idea is recorded but not yet
     * supported or contradicted by subsequent evidence.
     */
    CANDIDATE,

    /**
     * Early evidence supports the thesis.
     */
    EMERGING,

    /**
     * The thesis has played out broadly as expected.
     */
    CONFIRMED,

    /**
     * Evidence is turning against the thesis, but no invalidation
     * condition has been breached yet.
     */
    WEAKENING,

    /**
     * An invalidation condition was breached. The thesis was wrong,
     * or the situation changed materially.
     */
    INVALIDATED,

    /**
     * Closed. No further monitoring. Retained permanently for the
     * outcome record.
     */
    ARCHIVED;

    public boolean canTransitionTo(ThesisState target) {
        return allowedTransitions().contains(target);
    }

    public Set<ThesisState> allowedTransitions() {

        return switch (this) {

            // A draft is either published or abandoned.
            case DRAFT -> Set.of(CANDIDATE, ARCHIVED);

            case CANDIDATE -> Set.of(
                    EMERGING,
                    WEAKENING,
                    INVALIDATED,
                    ARCHIVED
            );

            case EMERGING -> Set.of(
                    CONFIRMED,
                    WEAKENING,
                    INVALIDATED,
                    ARCHIVED
            );

            // A confirmed thesis can still deteriorate later.
            case CONFIRMED -> Set.of(
                    WEAKENING,
                    INVALIDATED,
                    ARCHIVED
            );

            // Recovery from WEAKENING is permitted because weakening
            // is an observation, not a verdict.
            case WEAKENING -> Set.of(
                    EMERGING,
                    CONFIRMED,
                    INVALIDATED,
                    ARCHIVED
            );

            // Terminal outcome. Only archival remains. A new idea
            // about the same company requires a new thesis.
            case INVALIDATED -> Set.of(ARCHIVED);

            case ARCHIVED -> Set.of();
        };
    }

    public boolean isPublished() {
        return this != DRAFT;
    }

    public boolean isTerminal() {
        return this == ARCHIVED;
    }
}
