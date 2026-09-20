package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.MomentumResponse;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.repository.PriceBarRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class MomentumService {

    private final PriceBarRepository repository;

    public MomentumService(
            PriceBarRepository repository) {
        this.repository = repository;
    }

    public MomentumResponse getMomentum(
            UUID instrumentId) {

        List<PriceBar> prices =
                repository.findByInstrumentIdOrderByTradeDateDesc(
                        instrumentId);

        if (prices.size() < 2) {
            throw new IllegalArgumentException(
                    "At least 2 price bars required");
        }

        PriceBar latest = prices.get(0);
        PriceBar oldest = prices.get(prices.size() - 1);

        BigDecimal percentage =
                latest.getClosePrice()
                        .subtract(oldest.getClosePrice())
                        .divide(
                                oldest.getClosePrice(),
                                6,
                                RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        String trend =
                percentage.compareTo(BigDecimal.ZERO) > 0
                        ? "UPTREND"
                        : "DOWNTREND";

        return new MomentumResponse(
                latest.getInstrument().getSymbol(),
                oldest.getClosePrice(),
                latest.getClosePrice(),
                percentage,
                trend
        );
    }
}