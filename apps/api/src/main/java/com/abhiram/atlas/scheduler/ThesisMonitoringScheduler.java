package com.abhiram.atlas.scheduler;

import com.abhiram.atlas.dto.MonitoringRunResult;
import com.abhiram.atlas.service.ThesisMonitoringService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs thesis monitoring after the Indian market close.
 *
 * Scheduled for 18:45 IST on weekdays, which is after the price
 * refresh job so conditions are evaluated against the day's data
 * rather than yesterday's.
 *
 * Requires @EnableScheduling on the application class.
 */
@Component
public class ThesisMonitoringScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ThesisMonitoringScheduler.class);

    private final ThesisMonitoringService service;

    public ThesisMonitoringScheduler(
            ThesisMonitoringService service
    ) {
        this.service = service;
    }

    @Scheduled(
            cron = "0 45 18 * * MON-FRI",
            zone = "Asia/Kolkata"
    )
    public void monitorTheses() {

        log.info("Starting scheduled thesis monitoring");

        try {
            MonitoringRunResult result = service.monitorAll();

            log.info(
                    "Thesis monitoring complete. "
                            + "Monitored {}, new breaches on {}, "
                            + "invalidated {}",
                    result.thesesMonitored(),
                    result.thesesWithNewBreaches(),
                    result.thesesInvalidated()
            );

            result.results()
                    .stream()
                    .filter(item -> item.newBreaches() > 0)
                    .forEach(item -> log.warn(
                            "Thesis '{}' breached: {}",
                            item.title(),
                            String.join("; ", item.breachDetails())
                    ));

        } catch (RuntimeException ex) {
            // A scheduled job must never propagate. Losing one run is
            // acceptable, killing the scheduler thread is not.
            log.error(
                    "Scheduled thesis monitoring failed",
                    ex
            );
        }
    }
}
