package com.abhiram.atlas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A falsifiable condition that would invalidate the thesis.
 *
 * Conditions belong to a version rather than to the thesis, because a
 * revision may legitimately change what would prove the idea wrong.
 * Binding them to the version prevents quietly loosening a threshold
 * after it has already been breached.
 *
 * Example: metric OPERATING_MARGIN, comparison BELOW, threshold 17.00
 * reads as "invalidated if operating margin falls below 17 percent".
 */
@Entity
@Table(name = "thesis_condition")
public class ThesisCondition {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thesis_version_id", nullable = false)
    private ThesisVersion thesisVersion;

    @Column(name = "metric", nullable = false, length = 60)
    private String metric;

    @Column(name = "comparison", nullable = false, length = 20)
    private String comparison;

    @Column(name = "threshold", nullable = false, precision = 20, scale = 4)
    private BigDecimal threshold;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "breached", nullable = false)
    private Boolean breached;

    @Column(name = "breached_at")
    private LocalDateTime breachedAt;

    @Column(name = "breached_value", precision = 20, scale = 4)
    private BigDecimal breachedValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ThesisCondition() {
    }

    public static ThesisCondition create(
            UUID id,
            ThesisVersion version,
            String metric,
            String comparison,
            BigDecimal threshold,
            String description
    ) {
        ThesisCondition condition = new ThesisCondition();

        condition.id = id;
        condition.thesisVersion = version;
        condition.metric = metric;
        condition.comparison = comparison;
        condition.threshold = threshold;
        condition.description = description;
        condition.breached = Boolean.FALSE;
        condition.createdAt = LocalDateTime.now();

        return condition;
    }

    /**
     * Evaluates an observed value against this condition.
     *
     * A null observation returns false rather than treating missing
     * data as a breach. Absence of evidence is not evidence of
     * failure, and silently breaching on missing data would produce
     * false invalidations whenever a provider call fails.
     */
    public boolean evaluate(BigDecimal observedValue) {

        if (observedValue == null) {
            return false;
        }

        int comparisonResult =
                observedValue.compareTo(threshold);

        return switch (comparison.toUpperCase()) {
            case "BELOW" -> comparisonResult < 0;
            case "ABOVE" -> comparisonResult > 0;
            case "AT_OR_BELOW" -> comparisonResult <= 0;
            case "AT_OR_ABOVE" -> comparisonResult >= 0;
            default -> throw new IllegalStateException(
                    "Unsupported comparison: " + comparison
            );
        };
    }

    /**
     * Records a breach. Once breached, the original observation is
     * retained and cannot be overwritten by a later evaluation.
     */
    public void markBreached(BigDecimal observedValue) {

        if (Boolean.TRUE.equals(breached)) {
            return;
        }

        this.breached = Boolean.TRUE;
        this.breachedAt = LocalDateTime.now();
        this.breachedValue = observedValue;
    }

    public UUID getId() {
        return id;
    }

    public ThesisVersion getThesisVersion() {
        return thesisVersion;
    }

    public String getMetric() {
        return metric;
    }

    public String getComparison() {
        return comparison;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getBreached() {
        return breached;
    }

    public LocalDateTime getBreachedAt() {
        return breachedAt;
    }

    public BigDecimal getBreachedValue() {
        return breachedValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
