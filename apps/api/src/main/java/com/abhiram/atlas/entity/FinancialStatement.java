package com.abhiram.atlas.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_statement")
public class FinancialStatement {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "net_income")
    private BigDecimal netIncome;

    @Column(name = "operating_profit")
    private BigDecimal operatingProfit;

    @Column(name = "operating_cash_flow")
    private BigDecimal operatingCashFlow;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FinancialStatement() {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public static FinancialStatement fromProvider(
        UUID id,
        Company company,
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal operatingProfit,
        BigDecimal operatingCashFlow,
        LocalDateTime timestamp
) {
    FinancialStatement statement = new FinancialStatement();

    statement.id = id;
    statement.company = company;
    statement.fiscalYear = fiscalYear;
    statement.fiscalQuarter = fiscalQuarter;
    statement.revenue = revenue;
    statement.netIncome = netIncome;
    statement.operatingProfit = operatingProfit;
    statement.operatingCashFlow = operatingCashFlow;
    statement.createdAt = timestamp;
    statement.updatedAt = timestamp;

    return statement;
}
}