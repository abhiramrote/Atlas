package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.PriceBarResponse;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.repository.PriceBarRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PriceHistoryService {

    private final PriceBarRepository repository;

    public PriceHistoryService(
            PriceBarRepository repository) {
        this.repository = repository;
    }

    public List<PriceBarResponse> getPriceHistory(
            UUID instrumentId) {

        return repository
                .findByInstrumentIdOrderByTradeDateDesc(
                        instrumentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PriceBarResponse toResponse(
            PriceBar bar) {

        return new PriceBarResponse(
                bar.getTradeDate(),
                bar.getOpenPrice(),
                bar.getHighPrice(),
                bar.getLowPrice(),
                bar.getClosePrice(),
                bar.getVolume()
        );
    }
}