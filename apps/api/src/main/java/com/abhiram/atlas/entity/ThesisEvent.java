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
 * Append-only audit record of a thesis lifecycle event.
 *
 * These rows are never updated or deleted. The full history of how a
 * thesis evolved must remain reconstructable, including the decisions
 * that turned out to be wrong.
 */
@Entity
@Table(name = "thesis_event")
public class ThesisEvent {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thesis_id", nullable = false)
    private Thesis thesis;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 30)
    private ThesisState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", length = 30)
    private ThesisState toState;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected ThesisEvent() {
    }

    public static ThesisEvent record(
            UUID id,
            Thesis thesis,
            String eventType,
            ThesisState fromState,
            ThesisState toState,
            Integer versionNumber,
            String detail
    ) {
        ThesisEvent event = new ThesisEvent();

        event.id = id;
        event.thesis = thesis;
        event.eventType = eventType;
        event.fromState = fromState;
        event.toState = toState;
        event.versionNumber = versionNumber;
        event.detail = detail;
        event.occurredAt = LocalDateTime.now();

        return event;
    }

    public UUID getId() {
        return id;
    }

    public Thesis getThesis() {
        return thesis;
    }

    public String getEventType() {
        return eventType;
    }

    public ThesisState getFromState() {
        return fromState;
    }

    public ThesisState getToState() {
        return toState;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

