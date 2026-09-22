package com.abhiram.atlas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A financial figure as it was known on a specific date.
 *
 * The distinction that makes backtesting honest:
 *
 *   periodEndDate   when the reporting period ended
 *   knowledgeDate   when Atlas first learned the figure
 *
 * A company's FY2024 results end on 31 March 2024 but are not
 * published until roughly two months later. A backtest that scores
 * the company on 1 April 2024 using those figures is using
 * information that did not exist, and its results cannot be
 * reproduced in live trading.
 *
 * Restatements create a new observation rather than mutating this
 * one. The original keeps its knowledge date so the earlier view of
 * the world stays queryable.
 */
@Entity
@Table(name = "financial_observation")
public class FinancialObservation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(name = "knowledge_date", nullable = false)
    private LocalDate knowledgeDate;

    @Column(name = "revenue", precision = 20, scale = 2)
    private BigDecimal revenue;

    @Column(name = "net_income", precision = 20, scale = 2)
    private BigDecimal netIncome;

    @Column(name = "operating_profit", precision = 20, scale = 2)
    private BigDecimal operatingProfit;

    @Column(name = "operating_cash_flow", precision = 20, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "source_provider", nullable = false, length = 40)
    private String sourceProvider;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @Column(name = "superseded_by")
    private UUID supersededBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FinancialObservation() {
    }

    public static FinancialObservation record(
            UUID id,
            Company company,
            Integer fiscalYear,
            Integer fiscalQuarter,
            LocalDate periodEndDate,
            LocalDate knowledgeDate,
            BigDecimal revenue,
            BigDecimal netIncome,
            BigDecimal operatingProfit,
            BigDecimal operatingCashFlow,
            String sourceProvider,
            String unit,
            String currency,
            int revisionNumber
    ) {
        if (knowledgeDate == null) {
            throw new IllegalArgumentException(
                    "Knowledge date is required. Without it the "
                            + "observation cannot be used in a "
                            + "point-in-time query."
            );
        }

        FinancialObservation observation =
                new FinancialObservation();

        observation.id = id;
        observation.company = company;
        observation.fiscalYear = fiscalYear;
        observation.fiscalQuarter = fiscalQuarter;
        observation.periodEndDate = periodEndDate;
        observation.knowledgeDate = knowledgeDate;
        observation.revenue = revenue;
        observation.netIncome = netIncome;
        observation.operatingProfit = operatingProfit;
        observation.operatingCashFlow = operatingCashFlow;
        observation.sourceProvider = sourceProvider;
        observation.unit = unit;
        observation.currency = currency;
        observation.revisionNumber = revisionNumber;
        observation.createdAt = LocalDateTime.now();

        return observation;
    }

    /**
     * Marks this observation as replaced by a restatement.
     *
     * The figures themselves are never altered. A restatement is new
     * information, not a correction to what was previously known.
     */
    public void supersede(UUID replacementId) {

        if (supersededAt != null) {
            throw new IllegalStateException(
                    "Observation " + id + " is already superseded"
            );
        }

        this.supersededAt = LocalDateTime.now();
        this.supersededBy = replacementId;
    }

    /**
     * True when this observation was knowable on the given date.
     *
     * Used by point-in-time queries. An observation learned after the
     * evaluation date must be excluded even though its reporting
     * period may be much earlier.
     */
    public boolean wasKnownOn(LocalDate asOfDate) {
        return !knowledgeDate.isAfter(asOfDate);
    }

    public boolean isSuperseded() {
        return supersededAt != null;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public Integer getFiscalQuarter() {
        return fiscalQuarter;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public LocalDate getKnowledgeDate() {
        return knowledgeDate;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public BigDecimal getOperatingProfit() {
        return operatingProfit;
    }

    public BigDecimal getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public String getUnit() {
        return unit;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getRevisionNumber() {
        return revisionNumber;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    public UUID getSupersededBy() {
        return supersededBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
