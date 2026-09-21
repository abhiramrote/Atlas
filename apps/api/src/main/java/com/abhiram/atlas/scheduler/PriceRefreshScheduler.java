package com.abhiram.atlas.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.abhiram.atlas.service.PriceHistoryIngestionService;

@Component
public class PriceRefreshScheduler {

    private final PriceHistoryIngestionService service;

    public PriceRefreshScheduler(
            PriceHistoryIngestionService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void refreshPrices() {

        service.refreshAll();
    }
}