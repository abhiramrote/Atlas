package com.abhiram.atlas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "symbol", nullable = false, unique = true, length = 30)
    private String symbol;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "exchange", nullable = false, length = 30)
    private String exchange;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Instrument() {
    }

    public UUID getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getExchange() {
        return exchange;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}