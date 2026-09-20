package com.abhiram.atlas.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.MomentumResponse;
import com.abhiram.atlas.dto.TechnicalScoreResponse;

@Service
public class TechnicalScoreService {

    private final MomentumService momentumService;

    public TechnicalScoreService(
            MomentumService momentumService) {
        this.momentumService = momentumService;
    }

    public TechnicalScoreResponse score(
            UUID instrumentId) {

        MomentumResponse momentum =
                momentumService.getMomentum(
                        instrumentId);

        int score;

        if (momentum.priceChangePercent()
                .doubleValue() > 10) {

            score = 10;

        } else if (momentum.priceChangePercent()
                .doubleValue() > 5) {

            score = 7;

        } else if (momentum.priceChangePercent()
                .doubleValue() > 0) {

            score = 5;

        } else {

            score = 0;
        }

        return new TechnicalScoreResponse(
                momentum.symbol(),
                momentum.priceChangePercent(),
                score
        );
    }
}