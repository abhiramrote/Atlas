package com.abhiram.atlas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the thesis state machine.
 *
 * These encode the behavioural rules the feature exists to enforce,
 * so any future loosening has to be deliberate.
 */
class ThesisStateTest {

    @Test
    @DisplayName("A draft can only be published or abandoned")
    void draftCanOnlyPublishOrArchive() {

        assertThat(ThesisState.DRAFT.allowedTransitions())
                .containsExactlyInAnyOrder(
                        ThesisState.CANDIDATE,
                        ThesisState.ARCHIVED
                );
    }

    @Test
    @DisplayName("A draft cannot jump straight to confirmed")
    void draftCannotJumpToConfirmed() {

        assertThat(ThesisState.DRAFT
                .canTransitionTo(ThesisState.CONFIRMED))
                .isFalse();

        assertThat(ThesisState.DRAFT
                .canTransitionTo(ThesisState.EMERGING))
                .isFalse();
    }

    @Test
    @DisplayName("An invalidated thesis can never be un-invalidated")
    void invalidatedCannotReturnToPositiveState() {

        assertThat(ThesisState.INVALIDATED
                .canTransitionTo(ThesisState.CONFIRMED))
                .isFalse();

        assertThat(ThesisState.INVALIDATED
                .canTransitionTo(ThesisState.EMERGING))
                .isFalse();

        assertThat(ThesisState.INVALIDATED
                .canTransitionTo(ThesisState.WEAKENING))
                .isFalse();

        assertThat(ThesisState.INVALIDATED.allowedTransitions())
                .containsExactly(ThesisState.ARCHIVED);
    }

    @Test
    @DisplayName("A weakening thesis may recover")
    void weakeningCanRecover() {

        assertThat(ThesisState.WEAKENING
                .canTransitionTo(ThesisState.EMERGING))
                .isTrue();

        assertThat(ThesisState.WEAKENING
                .canTransitionTo(ThesisState.CONFIRMED))
                .isTrue();
    }

    @Test
    @DisplayName("A confirmed thesis can still deteriorate")
    void confirmedCanDeteriorate() {

        assertThat(ThesisState.CONFIRMED
                .canTransitionTo(ThesisState.WEAKENING))
                .isTrue();

        assertThat(ThesisState.CONFIRMED
                .canTransitionTo(ThesisState.INVALIDATED))
                .isTrue();
    }

    @Test
    @DisplayName("Archived is terminal")
    void archivedIsTerminal() {

        assertThat(ThesisState.ARCHIVED.allowedTransitions())
                .isEmpty();

        assertThat(ThesisState.ARCHIVED.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("Every state except draft counts as published")
    void publishedFlagIsCorrect() {

        assertThat(ThesisState.DRAFT.isPublished()).isFalse();

        assertThat(ThesisState.CANDIDATE.isPublished()).isTrue();
        assertThat(ThesisState.EMERGING.isPublished()).isTrue();
        assertThat(ThesisState.CONFIRMED.isPublished()).isTrue();
        assertThat(ThesisState.WEAKENING.isPublished()).isTrue();
        assertThat(ThesisState.INVALIDATED.isPublished()).isTrue();
        assertThat(ThesisState.ARCHIVED.isPublished()).isTrue();
    }

    @Test
    @DisplayName("Every state can reach archived except archived")
    void everyStateCanBeArchived() {

        for (ThesisState state : ThesisState.values()) {

            if (state == ThesisState.ARCHIVED) {
                continue;
            }

            assertThat(state
                    .canTransitionTo(ThesisState.ARCHIVED))
                    .as("%s should allow archiving", state)
                    .isTrue();
        }
    }
}
