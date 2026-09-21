package com.abhiram.atlas.entity;

import com.abhiram.atlas.domain.ThesisState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A research thesis about a company.
 *
 * This entity holds only mutable lifecycle state. The reasoning lives
 * in ThesisVersion, which is immutable once published.
 */
@Entity
@Table(name = "thesis")
public class Thesis {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private ThesisState state;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Thesis() {
    }

    public static Thesis open(
            UUID id,
            Company company,
            String title
    ) {
        LocalDateTime now = LocalDateTime.now();

        Thesis thesis = new Thesis();

        thesis.id = id;
        thesis.company = company;
        thesis.title = title;
        thesis.state = ThesisState.DRAFT;
        thesis.currentVersion = 0;
        thesis.openedAt = now;
        thesis.createdAt = now;
        thesis.updatedAt = now;

        return thesis;
    }

    /**
     * Applies a lifecycle transition.
     *
     * Invalid transitions are rejected here rather than in the service
     * so the rule travels with the entity and cannot be bypassed.
     */
    public void transitionTo(ThesisState target) {

        if (!state.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot move thesis from " + state
                            + " to " + target
            );
        }

        this.state = target;
        this.updatedAt = LocalDateTime.now();

        if (target.isTerminal()) {
            this.closedAt = this.updatedAt;
        }
    }

    public void recordNewVersion(int versionNumber) {
        this.currentVersion = versionNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public ThesisState getState() {
        return state;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
