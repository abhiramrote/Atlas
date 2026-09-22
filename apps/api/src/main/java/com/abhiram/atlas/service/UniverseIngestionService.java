package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.BulkIngestionItem;
import com.abhiram.atlas.dto.BulkIngestionResult;
import com.abhiram.atlas.dto.FundamentalsIngestionResult;
import com.abhiram.atlas.dto.ObservationIngestionResult;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.repository.InstrumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk fundamentals ingestion across the instrument universe.
 *
 * Design rules, learned from the price refresh failures:
 *
 * Failures are isolated per symbol. One provider rejection must not
 * abort the run or roll back companies that succeeded.
 *
 * Requests are paced. Free provider tiers rate limit aggressively,
 * and firing twenty calls in a second reliably triggers throttling
 * that looks like a data problem but is not.
 *
 * Both stores are written. financial_statement keeps current scoring
 * working, financial_observation accumulates dated history. Writing
 * only one would leave half the system stale.
 */
@Service
public class UniverseIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    UniverseIngestionService.class);

    /**
     * Gap between provider calls.
     *
     * Free tiers commonly allow a small number of requests per
     * minute. Pacing costs seconds and avoids an entire run failing
     * on throttling.
     */
    private static final Duration REQUEST_DELAY =
            Duration.ofMillis(1500);

    private final InstrumentRepository instrumentRepository;
    private final FundamentalsIngestionService fundamentalsService;
    private final ObservationIngestionService observationService;

    public UniverseIngestionService(
            InstrumentRepository instrumentRepository,
            FundamentalsIngestionService fundamentalsService,
            ObservationIngestionService observationService
    ) {
        this.instrumentRepository = instrumentRepository;
        this.fundamentalsService = fundamentalsService;
        this.observationService = observationService;
    }

    /**
     * Ingests fundamentals for every active instrument.
     *
     * Deliberately not transactional. Each symbol manages its own
     * transaction inside the delegated services, so a late failure
     * cannot discard earlier successes.
     */
    public BulkIngestionResult ingestUniverse() {

        List<Instrument> instruments = instrumentRepository.findAll()
                .stream()
                .filter(instrument ->
                        Boolean.TRUE.equals(instrument.getActive()))
                .toList();

        List<BulkIngestionItem> items = new ArrayList<>();

        Instant startedAt = Instant.now();

        for (int index = 0; index < instruments.size(); index++) {

            Instrument instrument = instruments.get(index);
            String symbol = instrument.getSymbol();

            items.add(ingestOne(symbol));

            boolean isLast = index == instruments.size() - 1;

            if (!isLast) {
                pause();
            }
        }

        Duration elapsed =
                Duration.between(startedAt, Instant.now());

        int succeeded = (int) items.stream()
                .filter(item -> "SUCCESS".equals(item.status()))
                .count();

        int skipped = (int) items.stream()
                .filter(item -> "SKIPPED".equals(item.status()))
                .count();

        int failed = items.size() - succeeded - skipped;

        log.info(
                "Universe ingestion complete. "
                        + "Total {}, succeeded {}, skipped {}, "
                        + "failed {}, elapsed {}s",
                items.size(),
                succeeded,
                skipped,
                failed,
                elapsed.toSeconds()
        );

        return new BulkIngestionResult(
                items.size(),
                succeeded,
                skipped,
                failed,
                elapsed.toSeconds(),
                items
        );
    }

    private BulkIngestionItem ingestOne(String symbol) {

        try {
            FundamentalsIngestionResult fundamentals =
                    fundamentalsService.ingest(symbol);

            // Observations are recorded even when the statement
            // ingest was skipped, because a company with a single
            // reported period still has a knowledge date worth
            // capturing for future point-in-time queries.
            ObservationIngestionResult observations =
                    observationService.ingest(symbol);

            return new BulkIngestionItem(
                    symbol,
                    fundamentals.status(),
                    fundamentals.periodsImported(),
                    observations.periodsRecorded(),
                    observations.restatements(),
                    fundamentals.message()
            );

        } catch (HttpClientErrorException ex) {

            log.warn(
                    "Provider rejected {}: {}",
                    symbol,
                    ex.getStatusCode()
            );

            return new BulkIngestionItem(
                    symbol,
                    "FAILED",
                    0,
                    0,
                    0,
                    describeProviderError(ex)
            );

        } catch (RestClientException ex) {

            return new BulkIngestionItem(
                    symbol,
                    "FAILED",
                    0,
                    0,
                    0,
                    "Provider request failed"
            );

        } catch (RuntimeException ex) {

            log.warn(
                    "Ingestion failed for {}: {}",
                    symbol,
                    ex.getMessage()
            );

            return new BulkIngestionItem(
                    symbol,
                    "FAILED",
                    0,
                    0,
                    0,
                    ex.getMessage() == null
                            ? "Unexpected failure"
                            : ex.getMessage()
            );
        }
    }

    /**
     * Paces requests without failing the run if interrupted.
     *
     * The interrupt flag is restored rather than swallowed so a
     * shutdown signal still propagates correctly.
     */
    private void pause() {
        try {
            Thread.sleep(REQUEST_DELAY.toMillis());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String describeProviderError(
            HttpClientErrorException ex
    ) {
        String body = ex.getResponseBodyAsString();

        if (body == null || body.isBlank()) {
            return "Provider returned HTTP "
                    + ex.getStatusCode().value();
        }

        // Provider error bodies can be long. Truncating keeps the
        // bulk response readable while preserving the cause.
        return body.length() > 200
                ? body.substring(0, 200) + "..."
                : body;
    }
}
