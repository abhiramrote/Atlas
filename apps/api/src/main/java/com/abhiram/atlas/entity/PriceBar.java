package com.abhiram.atlas.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "price_bar")
public class PriceBar {

    public PriceBar(
        UUID id,
        Instrument instrument,
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        LocalDateTime createdAt) {

    this.id = id;
    this.instrument = instrument;
    this.tradeDate = tradeDate;
    this.openPrice = openPrice;
    this.highPrice = highPrice;
    this.lowPrice = lowPrice;
    this.closePrice = closePrice;
    this.volume = volume;
    this.createdAt = createdAt;
}
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PriceBar() {
    }

    public UUID getId() {
        return id;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public Long getVolume() {
        return volume;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}