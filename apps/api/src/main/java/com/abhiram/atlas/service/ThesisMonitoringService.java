package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ThesisState;
import com.abhiram.atlas.dto.DataQualityReport;
import com.abhiram.atlas.dto.MonitoringRunResult;
import com.abhiram.atlas.dto.ThesisMonitoringResult;
import com.abhiram.atlas.entity.Thesis;
import com.abhiram.atlas.entity.ThesisCondition;
import com.abhiram.atlas.entity.ThesisEvent;
import com.abhiram.atlas.entity.ThesisVersion;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.ThesisConditionRepository;
import com.abhiram.atlas.repository.ThesisEventRepository;
import com.abhiram.atlas.repository.ThesisRepository;
import com.abhiram.atlas.repository.ThesisVersionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates stored invalidation conditions against current metrics.
 *
 * DATA QUALITY GATE
 *
 * This service will not auto-invalidate a thesis when the underlying
 * financial data is known to be unreliable.
 *
 * The reason is asymmetry of harm. A wrong score produces a bad
 * number that can be recalculated. A wrong invalidation destroys a
 * record permanently: the thesis moves to a terminal state, the
 * original reasoning is marked as disproven, and the state machine
 * forbids recovery. Since the entire value of the thesis engine rests
 * on that record being trustworthy, corrupting it is worse than
 * missing a genuine breach.
 *
 * HINDALCO demonstrated the risk concretely. Its FY2025 net income
 * was corrupted, producing a 1,856 percent growth figure. A thesis
 * with a net income growth condition would have breached on a number
 * that never happened, and the record would have shown the author was
 * wrong when they were not.
 *
 * When quality is suspect the thesis is instead moved to WEAKENING
 * with an explanation, which signals that something needs looking at
 * without asserting a verdict.
 *
 * Other behavioural rules:
 *
 * Missing data never breaches. Skipping is deliberate.
 * A breach is recorded once and never overwritten.
 * Only the current version is monitored.
 * Archived theses are skipped.
 */
@Service
public class ThesisMonitoringService {

    private static final Logger log =
            LoggerFactory.getLogger(ThesisMonitoringService.class);

    private final ThesisRepository thesisRepository;
    private final ThesisVersionRepository versionRepository;
    private final ThesisConditionRepository conditionRepository;
    private final ThesisEventRepository eventRepository;
    private final MetricResolver metricResolver;
    private final DataQualityService dataQualityService;

    public ThesisMonitoringService(
            ThesisRepository thesisRepository,
            ThesisVersionRepository versionRepository,
            ThesisConditionRepository conditionRepository,
            ThesisEventRepository eventRepository,
            MetricResolver metricResolver,
            DataQualityService dataQualityService
    ) {
        this.thesisRepository = thesisRepository;
        this.versionRepository = versionRepository;
        this.conditionRepository = conditionRepository;
        this.eventRepository = eventRepository;
        this.metricResolver = metricResolver;
        this.dataQualityService = dataQualityService;
    }

    public MonitoringRunResult monitorAll() {

        List<Thesis> open = thesisRepository.findAll()
                .stream()
                .filter(thesis ->
                        thesis.getState() != ThesisState.ARCHIVED)
                .toList();

        List<ThesisMonitoringResult> results = new ArrayList<>();

        for (Thesis thesis : open) {
            try {
                results.add(monitorThesis(thesis.getId()));

            } catch (RuntimeException ex) {
                log.warn(
                        "Monitoring failed for thesis {}: {}",
                        thesis.getId(),
                        ex.getMessage()
                );

                results.add(new ThesisMonitoringResult(
                        thesis.getId(),
                        thesis.getTitle(),
                        thesis.getState().name(),
                        thesis.getState().name(),
                        0,
                        0,
                        List.of(),
                        "Monitoring failed: " + ex.getMessage()
                ));
            }
        }

        int invalidated = (int) results.stream()
                .filter(result ->
                        ThesisState.INVALIDATED.name()
                                .equals(result.stateAfter())
                                && !ThesisState.INVALIDATED.name()
                                .equals(result.stateBefore()))
                .count();

        int withNewBreaches = (int) results.stream()
                .filter(result -> result.newBreaches() > 0)
                .count();

        return new MonitoringRunResult(
                results.size(),
                withNewBreaches,
                invalidated,
                results
        );
    }

    @Transactional
    public ThesisMonitoringResult monitorThesis(UUID thesisId) {

        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Thesis not found: " + thesisId
                        )
                );

        ThesisState stateBefore = thesis.getState();

        if (stateBefore == ThesisState.ARCHIVED) {
            return skipped(
                    thesis,
                    stateBefore,
                    "Thesis is archived and no longer monitored"
            );
        }

        ThesisVersion currentVersion = versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(thesisId)
                .orElse(null);

        if (currentVersion == null) {
            return skipped(
                    thesis,
                    stateBefore,
                    "No published version to monitor"
            );
        }

        List<ThesisCondition> conditions = conditionRepository
                .findByThesisVersionId(currentVersion.getId());

        if (conditions.isEmpty()) {
            return skipped(
                    thesis,
                    stateBefore,
                    "No invalidation conditions on current version"
            );
        }

        // Quality is assessed before any condition is evaluated, so
        // a corrupted figure cannot reach the invalidation path.
        DataQualityReport quality = dataQualityService.check(
                thesis.getCompany().getId()
        );

        boolean dataReliable =
                Boolean.TRUE.equals(quality.reliableForScoring());

        Map<String, BigDecimal> metrics =
                metricResolver.resolveAll(thesis.getCompany());

        int evaluated = 0;
        List<String> newBreachDetails = new ArrayList<>();

        for (ThesisCondition condition : conditions) {

            if (Boolean.TRUE.equals(condition.getBreached())) {
                continue;
            }

            if (!metricResolver.isSupported(condition.getMetric())) {
                continue;
            }

            BigDecimal observed =
                    metrics.get(condition.getMetric());

            if (observed == null) {
                continue;
            }

            evaluated++;

            if (!condition.evaluate(observed)) {
                continue;
            }

            String detail = describeBreach(condition, observed);

            if (!dataReliable) {

                // The condition appears breached, but the figure it
                // was measured against cannot be trusted. Recording
                // the breach would permanently mark the thesis as
                // disproven on evidence that may not exist.
                newBreachDetails.add(
                        "Suspected breach not recorded: " + detail
                );

                continue;
            }

            condition.markBreached(observed);
            conditionRepository.save(condition);

            newBreachDetails.add(detail);

            recordEvent(
                    thesis,
                    "CONDITION_BREACHED",
                    stateBefore,
                    stateBefore,
                    currentVersion.getVersionNumber(),
                    detail
            );
        }

        if (newBreachDetails.isEmpty()) {
            return new ThesisMonitoringResult(
                    thesis.getId(),
                    thesis.getTitle(),
                    stateBefore.name(),
                    stateBefore.name(),
                    evaluated,
                    0,
                    List.of(),
                    dataReliable
                            ? null
                            : "Data quality issues prevented a "
                                    + "reliable assessment: "
                                    + quality.summary()
            );
        }

        if (!dataReliable) {
            return handleSuspectBreach(
                    thesis,
                    stateBefore,
                    currentVersion,
                    evaluated,
                    newBreachDetails,
                    quality
            );
        }

        return handleConfirmedBreach(
                thesis,
                stateBefore,
                currentVersion,
                evaluated,
                newBreachDetails
        );
    }

    /**
     * Handles a breach measured against unreliable data.
     *
     * The thesis moves to WEAKENING rather than INVALIDATED, because
     * WEAKENING is an observation that something needs attention
     * while INVALIDATED is a verdict. The state machine permits
     * recovery from WEAKENING, so a false alarm caused by bad
     * provider data does not permanently damage the record.
     */
    private ThesisMonitoringResult handleSuspectBreach(
            Thesis thesis,
            ThesisState stateBefore,
            ThesisVersion currentVersion,
            int evaluated,
            List<String> details,
            DataQualityReport quality
    ) {
        ThesisState stateAfter = stateBefore;

        String note = "Conditions appear breached but the financial "
                + "data is unreliable, so the thesis was not "
                + "invalidated. " + quality.summary();

        if (stateBefore.canTransitionTo(ThesisState.WEAKENING)) {

            thesis.transitionTo(ThesisState.WEAKENING);
            thesisRepository.save(thesis);

            stateAfter = ThesisState.WEAKENING;

            recordEvent(
                    thesis,
                    "SUSPECTED_BREACH",
                    stateBefore,
                    ThesisState.WEAKENING,
                    currentVersion.getVersionNumber(),
                    "Moved to weakening rather than invalidated "
                            + "because "
                            + quality.errorCount()
                            + " data quality error(s) affect the "
                            + "periods used for evaluation. Verify "
                            + "the source figures before treating "
                            + "this thesis as disproven."
            );
        } else {

            recordEvent(
                    thesis,
                    "SUSPECTED_BREACH",
                    stateBefore,
                    stateBefore,
                    currentVersion.getVersionNumber(),
                    "Suspected breach recorded without a state "
                            + "change. " + quality.summary()
            );
        }

        log.warn(
                "Thesis '{}' has a suspected breach on unreliable "
                        + "data. Not invalidated.",
                thesis.getTitle()
        );

        return new ThesisMonitoringResult(
                thesis.getId(),
                thesis.getTitle(),
                stateBefore.name(),
                stateAfter.name(),
                evaluated,
                0,
                details,
                note
        );
    }

    private ThesisMonitoringResult handleConfirmedBreach(
            Thesis thesis,
            ThesisState stateBefore,
            ThesisVersion currentVersion,
            int evaluated,
            List<String> details
    ) {
        ThesisState stateAfter = stateBefore;
        String note;

        if (stateBefore.canTransitionTo(
                ThesisState.INVALIDATED)) {

            thesis.transitionTo(ThesisState.INVALIDATED);
            thesisRepository.save(thesis);

            stateAfter = ThesisState.INVALIDATED;

            recordEvent(
                    thesis,
                    "AUTO_INVALIDATED",
                    stateBefore,
                    ThesisState.INVALIDATED,
                    currentVersion.getVersionNumber(),
                    "Automatically invalidated because "
                            + details.size()
                            + " pre-committed condition"
                            + (details.size() == 1
                                    ? " was" : "s were")
                            + " breached on verified data"
            );

            note = "Thesis invalidated by its own conditions";

        } else {
            note = "Conditions breached but the thesis could not "
                    + "transition from " + stateBefore;
        }

        return new ThesisMonitoringResult(
                thesis.getId(),
                thesis.getTitle(),
                stateBefore.name(),
                stateAfter.name(),
                evaluated,
                details.size(),
                details,
                note
        );
    }

    private ThesisMonitoringResult skipped(
            Thesis thesis,
            ThesisState state,
            String note
    ) {
        return new ThesisMonitoringResult(
                thesis.getId(),
                thesis.getTitle(),
                state.name(),
                state.name(),
                0,
                0,
                List.of(),
                note
        );
    }

    private String describeBreach(
            ThesisCondition condition,
            BigDecimal observed
    ) {
        String readable = condition.getMetric()
                .replace('_', ' ')
                .toLowerCase();

        String operator = switch (condition.getComparison()) {
            case "BELOW" -> "fell below";
            case "ABOVE" -> "rose above";
            case "AT_OR_BELOW" -> "reached or fell below";
            case "AT_OR_ABOVE" -> "reached or rose above";
            default -> "breached";
        };

        return String.format(
                "%s %s %s (observed %s)",
                capitalise(readable),
                operator,
                condition.getThreshold().toPlainString(),
                observed.toPlainString()
        );
    }

    private String capitalise(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return Character.toUpperCase(value.charAt(0))
                + value.substring(1);
    }

    private void recordEvent(
            Thesis thesis,
            String eventType,
            ThesisState fromState,
            ThesisState toState,
            Integer versionNumber,
            String detail
    ) {
        eventRepository.save(ThesisEvent.record(
                UUID.randomUUID(),
                thesis,
                eventType,
                fromState,
                toState,
                versionNumber,
                detail
        ));
    }
}
