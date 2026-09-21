package com.abhiram.atlas.service;

import com.abhiram.atlas.domain.ThesisState;
import com.abhiram.atlas.dto.ConditionRequest;
import com.abhiram.atlas.dto.ConditionResponse;
import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.dto.PublishThesisRequest;
import com.abhiram.atlas.dto.ReviseThesisRequest;
import com.abhiram.atlas.dto.ThesisEventResponse;
import com.abhiram.atlas.dto.ThesisResponse;
import com.abhiram.atlas.dto.ThesisVersionResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.PriceBar;
import com.abhiram.atlas.entity.Thesis;
import com.abhiram.atlas.entity.ThesisCondition;
import com.abhiram.atlas.entity.ThesisEvent;
import com.abhiram.atlas.entity.ThesisVersion;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.PriceBarRepository;
import com.abhiram.atlas.repository.ThesisEventRepository;
import com.abhiram.atlas.repository.ThesisRepository;
import com.abhiram.atlas.repository.ThesisVersionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thesis lifecycle management.
 *
 * Core guarantee: a published thesis version is never modified. A
 * revision creates a new version and supersedes the previous one, so
 * the original judgement stays inspectable. This exists specifically
 * to prevent the most common failure in personal investing, which is
 * unconsciously rewriting your reasoning after the outcome is known.
 */
@Service
public class ThesisService {

    private static final Set<String> VALID_CONVICTIONS =
            Set.of("LOW", "MEDIUM", "HIGH");

    private static final Set<String> VALID_COMPARISONS =
            Set.of("BELOW", "ABOVE", "AT_OR_BELOW", "AT_OR_ABOVE");

    private final ThesisRepository thesisRepository;
    private final ThesisVersionRepository versionRepository;
    private final ThesisEventRepository eventRepository;
    private final CompanyRepository companyRepository;
    private final PriceBarRepository priceBarRepository;
    private final OpportunityScoreV2Service scoreService;

    public ThesisService(
            ThesisRepository thesisRepository,
            ThesisVersionRepository versionRepository,
            ThesisEventRepository eventRepository,
            CompanyRepository companyRepository,
            PriceBarRepository priceBarRepository,
            OpportunityScoreV2Service scoreService
    ) {
        this.thesisRepository = thesisRepository;
        this.versionRepository = versionRepository;
        this.eventRepository = eventRepository;
        this.companyRepository = companyRepository;
        this.priceBarRepository = priceBarRepository;
        this.scoreService = scoreService;
    }

    /**
     * Creates a thesis and publishes its first version in one step.
     *
     * There is no separate draft stage in this release. A thesis is
     * either committed or it does not exist, which keeps the record
     * honest.
     */
    @Transactional
    public ThesisResponse publish(PublishThesisRequest request) {

        validateConviction(request.conviction());
        validateConditions(request.invalidationConditions());

        Company company = companyRepository
                .findById(request.companyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: "
                                        + request.companyId()
                        )
                );

        Thesis thesis = Thesis.open(
                UUID.randomUUID(),
                company,
                request.title()
        );

        thesisRepository.save(thesis);

        ThesisVersion version = createVersion(
                thesis,
                1,
                request.horizonMonths(),
                request.conviction(),
                request.rationale(),
                request.keyRisks(),
                request.invalidationConditions()
        );

        thesis.recordNewVersion(1);
        thesis.transitionTo(ThesisState.CANDIDATE);

        thesisRepository.save(thesis);

        recordEvent(
                thesis,
                "PUBLISHED",
                ThesisState.DRAFT,
                ThesisState.CANDIDATE,
                1,
                "Thesis published with "
                        + request.invalidationConditions().size()
                        + " invalidation conditions"
        );

        return toResponse(thesis, version);
    }

    /**
     * Publishes a revised version.
     *
     * The previous version is superseded rather than replaced. Both
     * remain queryable, and the revision reason is captured in the
     * audit trail so a change of mind is itself a recorded fact.
     */
    @Transactional
    public ThesisResponse revise(
            UUID thesisId,
            ReviseThesisRequest request
    ) {
        validateConviction(request.conviction());
        validateConditions(request.invalidationConditions());

        Thesis thesis = loadThesis(thesisId);

        if (thesis.getState().isTerminal()) {
            throw new IllegalStateException(
                    "An archived thesis cannot be revised. "
                            + "Open a new thesis instead."
            );
        }

        ThesisVersion previous = versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(thesisId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No published version found for "
                                        + "thesis: " + thesisId
                        )
                );

        previous.supersede();
        versionRepository.save(previous);

        int nextVersionNumber = previous.getVersionNumber() + 1;

        ThesisVersion version = createVersion(
                thesis,
                nextVersionNumber,
                request.horizonMonths(),
                request.conviction(),
                request.rationale(),
                request.keyRisks(),
                request.invalidationConditions()
        );

        thesis.recordNewVersion(nextVersionNumber);
        thesisRepository.save(thesis);

        recordEvent(
                thesis,
                "REVISED",
                thesis.getState(),
                thesis.getState(),
                nextVersionNumber,
                request.revisionReason()
        );

        return toResponse(thesis, version);
    }

    /**
     * Applies a lifecycle transition.
     *
     * Transition legality is enforced by the entity. A reason is
     * mandatory so the audit trail explains why, not just what.
     */
    @Transactional
    public ThesisResponse transition(
            UUID thesisId,
            String targetStateName,
            String reason
    ) {
        Thesis thesis = loadThesis(thesisId);

        ThesisState target = parseState(targetStateName);
        ThesisState origin = thesis.getState();

        thesis.transitionTo(target);
        thesisRepository.save(thesis);

        recordEvent(
                thesis,
                "TRANSITION",
                origin,
                target,
                thesis.getCurrentVersion(),
                reason
        );

        return toResponse(thesis, currentVersion(thesis));
    }

    @Transactional(readOnly = true)
    public ThesisResponse getThesis(UUID thesisId) {
        Thesis thesis = loadThesis(thesisId);
        return toResponse(thesis, currentVersion(thesis));
    }

    @Transactional(readOnly = true)
    public List<ThesisResponse> getAllTheses() {

        return thesisRepository.findAll()
                .stream()
                .map(thesis ->
                        toResponse(thesis, currentVersion(thesis)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThesisResponse> getThesesForCompany(
            UUID companyId
    ) {
        return thesisRepository
                .findByCompanyIdOrderByOpenedAtDesc(companyId)
                .stream()
                .map(thesis ->
                        toResponse(thesis, currentVersion(thesis)))
                .toList();
    }

    /**
     * Full version history, newest first. This is the endpoint that
     * makes past reasoning auditable.
     */
    @Transactional(readOnly = true)
    public List<ThesisVersionResponse> getVersionHistory(
            UUID thesisId
    ) {
        loadThesis(thesisId);

        return versionRepository
                .findByThesisIdOrderByVersionNumberDesc(thesisId)
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThesisEventResponse> getAuditTrail(UUID thesisId) {

        loadThesis(thesisId);

        return eventRepository
                .findByThesisIdOrderByOccurredAtAsc(thesisId)
                .stream()
                .map(event -> new ThesisEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getFromState() == null
                                ? null
                                : event.getFromState().name(),
                        event.getToState() == null
                                ? null
                                : event.getToState().name(),
                        event.getVersionNumber(),
                        event.getDetail(),
                        event.getOccurredAt()
                ))
                .toList();
    }

    // ----------------------------------------------------------
    // Internals
    // ----------------------------------------------------------

    private ThesisVersion createVersion(
            Thesis thesis,
            int versionNumber,
            int horizonMonths,
            String conviction,
            String rationale,
            String keyRisks,
            List<ConditionRequest> conditionRequests
    ) {
        ScoreSnapshot snapshot =
                captureSnapshot(thesis.getCompany());

        ThesisVersion version = ThesisVersion.publish(
                UUID.randomUUID(),
                thesis,
                versionNumber,
                horizonMonths,
                conviction.toUpperCase(),
                rationale,
                keyRisks,
                snapshot.fundamentalScore(),
                snapshot.technicalScore(),
                snapshot.finalScore(),
                snapshot.rating(),
                snapshot.policyVersion(),
                snapshot.closePrice()
        );

        for (ConditionRequest request : conditionRequests) {

            ThesisCondition condition = ThesisCondition.create(
                    UUID.randomUUID(),
                    version,
                    request.metric().toUpperCase(),
                    request.comparison().toUpperCase(),
                    request.threshold(),
                    request.description()
            );

            version.addCondition(condition);
        }

        return versionRepository.save(version);
    }

    /**
     * Captures the scores and price visible at publication.
     *
     * Failures are tolerated deliberately. A missing score should not
     * block someone from recording their reasoning, and an incomplete
     * snapshot is more useful than no thesis at all.
     */
    private ScoreSnapshot captureSnapshot(Company company) {

        Integer fundamentalScore = null;
        Integer technicalScore = null;
        Integer finalScore = null;
        String rating = null;
        String policyVersion = null;

        try {
            OpportunityScoreV2Response score =
                    scoreService.score(company.getId());

            fundamentalScore = score.fundamentalScore();
            technicalScore = score.technicalScore();
            finalScore = score.finalScore();
            rating = score.rating();
            policyVersion = score.policyVersion();

        } catch (RuntimeException ignored) {
            // Snapshot remains partially populated.
        }

        BigDecimal closePrice = priceBarRepository
                .findByInstrumentIdOrderByTradeDateDesc(
                        company.getInstrument().getId())
                .stream()
                .findFirst()
                .map(PriceBar::getClosePrice)
                .orElse(null);

        return new ScoreSnapshot(
                fundamentalScore,
                technicalScore,
                finalScore,
                rating,
                policyVersion,
                closePrice
        );
    }

    private Thesis loadThesis(UUID thesisId) {

        return thesisRepository.findById(thesisId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Thesis not found: " + thesisId
                        )
                );
    }

    private ThesisVersion currentVersion(Thesis thesis) {

        return versionRepository
                .findFirstByThesisIdOrderByVersionNumberDesc(
                        thesis.getId())
                .orElse(null);
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

    private void validateConviction(String conviction) {

        if (conviction == null
                || !VALID_CONVICTIONS.contains(
                        conviction.toUpperCase())) {

            throw new IllegalArgumentException(
                    "Conviction must be one of "
                            + VALID_CONVICTIONS
            );
        }
    }

    private void validateConditions(
            List<ConditionRequest> conditions
    ) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one invalidation condition is "
                            + "required"
            );
        }

        for (ConditionRequest condition : conditions) {

            if (condition.comparison() == null
                    || !VALID_COMPARISONS.contains(
                            condition.comparison().toUpperCase())) {

                throw new IllegalArgumentException(
                        "Comparison must be one of "
                                + VALID_COMPARISONS
                );
            }
        }
    }

    private ThesisState parseState(String value) {

        try {
            return ThesisState.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown thesis state: " + value
            );
        }
    }

    private ThesisResponse toResponse(
            Thesis thesis,
            ThesisVersion version
    ) {
        LocalDate horizonEnd = null;

        if (version != null) {
            horizonEnd = version.getPublishedAt()
                    .toLocalDate()
                    .plusMonths(version.getHorizonMonths());
        }

        List<String> allowed = new ArrayList<>(
                thesis.getState()
                        .allowedTransitions()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .toList()
        );

        return new ThesisResponse(
                thesis.getId(),
                thesis.getCompany().getId(),
                thesis.getCompany().getInstrument().getSymbol(),
                thesis.getCompany().getInstrument().getCompanyName(),
                thesis.getTitle(),
                thesis.getState().name(),
                thesis.getCurrentVersion(),
                thesis.getOpenedAt(),
                thesis.getClosedAt(),
                horizonEnd,
                allowed,
                version == null
                        ? null
                        : toVersionResponse(version)
        );
    }

    private ThesisVersionResponse toVersionResponse(
            ThesisVersion version
    ) {
        List<ConditionResponse> conditions =
                version.getConditions()
                        .stream()
                        .map(condition -> new ConditionResponse(
                                condition.getId(),
                                condition.getMetric(),
                                condition.getComparison(),
                                condition.getThreshold(),
                                condition.getDescription(),
                                Boolean.TRUE.equals(
                                        condition.getBreached()),
                                condition.getBreachedAt(),
                                condition.getBreachedValue()
                        ))
                        .toList();

        return new ThesisVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getHorizonMonths(),
                version.getConviction(),
                version.getRationale(),
                version.getKeyRisks(),
                version.getSnapshotFundamentalScore(),
                version.getSnapshotTechnicalScore(),
                version.getSnapshotFinalScore(),
                version.getSnapshotRating(),
                version.getSnapshotPolicyVersion(),
                version.getSnapshotClosePrice(),
                version.getPublishedAt(),
                version.getSupersededAt(),
                version.isSuperseded(),
                conditions
        );
    }

    private record ScoreSnapshot(
            Integer fundamentalScore,
            Integer technicalScore,
            Integer finalScore,
            String rating,
            String policyVersion,
            BigDecimal closePrice
    ) {
    }
}
