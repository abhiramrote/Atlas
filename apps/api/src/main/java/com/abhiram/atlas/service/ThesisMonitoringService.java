package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ThesisState;
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
 * Behavioural rules, in order of importance:
 *
 * 1. Missing data never breaches. If a metric cannot be computed the
 *    condition is skipped. Treating unknown as failure would produce
 *    false invalidations whenever a provider call fails, and a
 *    monitoring system that cries wolf is worse than none.
 *
 * 2. A breach is recorded once. Re-running the job does not overwrite
 *    the first observed value or duplicate audit events. When the
 *    thesis first broke is the fact that matters.
 *
 * 3. Only the current version is monitored. Superseded versions are
 *    historical record and their conditions are frozen.
 *
 * 4. Breaching moves the thesis to INVALIDATED automatically, because
 *    the user already decided in advance that this outcome disproves
 *    the idea. Honouring that pre-commitment is the entire point.
 *
 * 5. Archived theses are skipped. They are closed.
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

    public ThesisMonitoringService(
            ThesisRepository thesisRepository,
            ThesisVersionRepository versionRepository,
            ThesisConditionRepository conditionRepository,
            ThesisEventRepository eventRepository,
            MetricResolver metricResolver
    ) {
        this.thesisRepository = thesisRepository;
        this.versionRepository = versionRepository;
        this.conditionRepository = conditionRepository;
        this.eventRepository = eventRepository;
        this.metricResolver = metricResolver;
    }

    /**
     * Monitors every thesis that is still open.
     *
     * Each thesis is evaluated in its own transaction so that one
     * failure cannot roll back breaches correctly recorded for
     * another thesis.
     */
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
            return new ThesisMonitoringResult(
                    thesis.getId(),
                    thesis.getTitle(),
                    stateBefore.name(),
                    stateBefore.name(),
                    0,
                    0,
                    List.of(),
                    "Thesis is archived and no longer monitored"
            );
        }

        ThesisVersion currentVersion = versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(thesisId)
                .orElse(null);

        if (currentVersion == null) {
            return new ThesisMonitoringResult(
                    thesis.getId(),
                    thesis.getTitle(),
                    stateBefore.name(),
                    stateBefore.name(),
                    0,
                    0,
                    List.of(),
                    "No published version to monitor"
            );
        }

        List<ThesisCondition> conditions = conditionRepository
                .findByThesisVersionId(currentVersion.getId());

        if (conditions.isEmpty()) {
            return new ThesisMonitoringResult(
                    thesis.getId(),
                    thesis.getTitle(),
                    stateBefore.name(),
                    stateBefore.name(),
                    0,
                    0,
                    List.of(),
                    "No invalidation conditions on current version"
            );
        }

        Map<String, BigDecimal> metrics =
                metricResolver.resolveAll(thesis.getCompany());

        int evaluated = 0;
        List<String> newBreachDetails = new ArrayList<>();

        for (ThesisCondition condition : conditions) {

            if (Boolean.TRUE.equals(condition.getBreached())) {
                // Already broken. Do not re-record or overwrite the
                // original observation.
                continue;
            }

            if (!metricResolver.isSupported(condition.getMetric())) {
                log.debug(
                        "Unsupported metric {} on thesis {}",
                        condition.getMetric(),
                        thesisId
                );
                continue;
            }

            BigDecimal observed =
                    metrics.get(condition.getMetric());

            if (observed == null) {
                // Unknown stays unknown. Skipping is deliberate.
                continue;
            }

            evaluated++;

            if (condition.evaluate(observed)) {

                condition.markBreached(observed);
                conditionRepository.save(condition);

                String detail = describeBreach(condition, observed);
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
        }

        ThesisState stateAfter = stateBefore;
        String note = null;

        if (!newBreachDetails.isEmpty()
                && stateBefore.canTransitionTo(
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
                            + newBreachDetails.size()
                            + " pre-committed condition"
                            + (newBreachDetails.size() == 1
                                    ? " was" : "s were")
                            + " breached"
            );

            note = "Thesis invalidated by its own conditions";

        } else if (!newBreachDetails.isEmpty()) {
            note = "Conditions breached but the thesis could not "
                    + "transition from " + stateBefore;
        }

        return new ThesisMonitoringResult(
                thesis.getId(),
                thesis.getTitle(),
                stateBefore.name(),
                stateAfter.name(),
                evaluated,
                newBreachDetails.size(),
                newBreachDetails,
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
