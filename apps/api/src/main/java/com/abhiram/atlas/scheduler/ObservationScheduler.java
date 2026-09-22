package com.abhiram.atlas.scheduler;

import com.abhiram.atlas.dto.BulkIngestionResult;
import com.abhiram.atlas.service.UniverseIngestionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily fundamentals refresh and observation recording.
 *
 * WHY THIS MATTERS MORE THAN IT LOOKS
 *
 * Atlas currently has no usable point-in-time history. Every
 * observation shares one knowledge date, so any backtest before that
 * date correctly returns nothing.
 *
 * This job is the only honest way to fix that. Each run records what
 * the provider reported on that day, building genuine knowledge dates
 * over time. After a year there is a year of real history that was
 * never reconstructed or approximated.
 *
 * There is no shortcut. Backdating observations to their period end
 * dates would produce history that looks usable and is not, which is
 * worse than having none because it would be trusted.
 *
 * Scheduled for 07:00 IST, before market open, so a refresh cannot
 * land midway through a trading session and shift scores while they
 * are being read.
 */
@Component
public class ObservationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ObservationScheduler.class);

    private final UniverseIngestionService service;

    @Value("${atlas.scheduler.fundamentals.enabled:true}")
    private boolean enabled;

    public ObservationScheduler(
            UniverseIngestionService service
    ) {
        this.service = service;
    }

    @Scheduled(
            cron = "0 0 7 * * MON-FRI",
            zone = "Asia/Kolkata"
    )
    public void recordDailyObservations() {

        if (!enabled) {
            log.debug("Fundamentals scheduler is disabled");
            return;
        }

        log.info("Starting daily fundamentals observation run");

        try {
            BulkIngestionResult result =
                    service.ingestUniverse();

            log.info(
                    "Observation run complete. "
                            + "Total {}, succeeded {}, "
                            + "skipped {}, failed {}",
                    result.total(),
                    result.succeeded(),
                    result.skipped(),
                    result.failed()
            );

            // Restatements deserve a louder log line. They mean a
            // provider changed a previously reported figure, which
            // invalidates any score computed from the old one.
            result.results()
                    .stream()
                    .filter(item ->
                            item.restatements() != null
                                    && item.restatements() > 0)
                    .forEach(item -> log.warn(
                            "Restatement detected for {}: {} period(s) "
                                    + "changed",
                            item.symbol(),
                            item.restatements()
                    ));

        } catch (RuntimeException ex) {
            // A scheduled job must never propagate. Losing one run is
            // recoverable. Killing the scheduler thread is not.
            log.error(
                    "Daily observation run failed",
                    ex
            );
        }
    }
}
