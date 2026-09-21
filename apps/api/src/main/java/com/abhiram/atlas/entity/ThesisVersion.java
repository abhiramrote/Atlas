package com.abhiram.atlas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An immutable snapshot of a thesis at the moment it was published.
 *
 * Nothing here is editable after construction except supersededAt,
 * which marks that a later version has replaced this one. Revising a
 * thesis creates a new version rather than mutating this record.
 *
 * The score and price fields are stored rather than recomputed. The
 * underlying financial data can be restated and the scoring policy can
 * change, so the only way to judge a past thesis fairly is against the
 * numbers that were visible when it was written.
 */
@Entity
@Table(name = "thesis_version")
public class ThesisVersion {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thesis_id", nullable = false)
    private Thesis thesis;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "horizon_months", nullable = false)
    private Integer horizonMonths;

    @Column(name = "conviction", nullable = false, length = 20)
    private String conviction;

    @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "key_risks", columnDefinition = "TEXT")
    private String keyRisks;

    @Column(name = "snapshot_fundamental_score")
    private Integer snapshotFundamentalScore;

    @Column(name = "snapshot_technical_score")
    private Integer snapshotTechnicalScore;

    @Column(name = "snapshot_final_score")
    private Integer snapshotFinalScore;

    @Column(name = "snapshot_rating", length = 20)
    private String snapshotRating;

    @Column(name = "snapshot_policy_version", length = 20)
    private String snapshotPolicyVersion;

    @Column(name = "snapshot_close_price", precision = 20, scale = 4)
    private BigDecimal snapshotClosePrice;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "thesisVersion",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<ThesisCondition> conditions = new ArrayList<>();

    protected ThesisVersion() {
    }

    public static ThesisVersion publish(
            UUID id,
            Thesis thesis,
            int versionNumber,
            int horizonMonths,
            String conviction,
            String rationale,
            String keyRisks,
            Integer fundamentalScore,
            Integer technicalScore,
            Integer finalScore,
            String rating,
            String policyVersion,
            BigDecimal closePrice
    ) {
        LocalDateTime now = LocalDateTime.now();

        ThesisVersion version = new ThesisVersion();

        version.id = id;
        version.thesis = thesis;
        version.versionNumber = versionNumber;
        version.horizonMonths = horizonMonths;
        version.conviction = conviction;
        version.rationale = rationale;
        version.keyRisks = keyRisks;
        version.snapshotFundamentalScore = fundamentalScore;
        version.snapshotTechnicalScore = technicalScore;
        version.snapshotFinalScore = finalScore;
        version.snapshotRating = rating;
        version.snapshotPolicyVersion = policyVersion;
        version.snapshotClosePrice = closePrice;
        version.publishedAt = now;
        version.createdAt = now;

        return version;
    }

    /**
     * Marks this version as replaced. This is the only permitted
     * mutation, and it does not alter the recorded reasoning.
     */
    public void supersede() {

        if (supersededAt != null) {
            throw new IllegalStateException(
                    "Version " + versionNumber
                            + " is already superseded"
            );
        }

        this.supersededAt = LocalDateTime.now();
    }

    public void addCondition(ThesisCondition condition) {
        conditions.add(condition);
    }

    public boolean isSuperseded() {
        return supersededAt != null;
    }

    public UUID getId() {
        return id;
    }

    public Thesis getThesis() {
        return thesis;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Integer getHorizonMonths() {
        return horizonMonths;
    }

    public String getConviction() {
        return conviction;
    }

    public String getRationale() {
        return rationale;
    }

    public String getKeyRisks() {
        return keyRisks;
    }

    public Integer getSnapshotFundamentalScore() {
        return snapshotFundamentalScore;
    }

    public Integer getSnapshotTechnicalScore() {
        return snapshotTechnicalScore;
    }

    public Integer getSnapshotFinalScore() {
        return snapshotFinalScore;
    }

    public String getSnapshotRating() {
        return snapshotRating;
    }

    public String getSnapshotPolicyVersion() {
        return snapshotPolicyVersion;
    }

    public BigDecimal getSnapshotClosePrice() {
        return snapshotClosePrice;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ThesisCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }
}
