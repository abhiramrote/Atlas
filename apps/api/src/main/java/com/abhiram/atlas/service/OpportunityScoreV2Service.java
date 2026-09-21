package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.dto.TechnicalScoreResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Combines the fundamental opportunity score with the technical score.
 *
 * Design decision (M5):
 * A missing technical score must not discard an otherwise valid
 * fundamental score. Price history depends on an external provider and
 * can be unavailable for reasons unrelated to company quality, such as
 * provider plan restrictions or a symbol that has not been ingested yet.
 *
 * When the technical component cannot be computed, this service returns
 * the fundamental score alone and marks the result as partially
 * available. The final score is then not comparable with a fully
 * scored company, so callers should display the availability flag.
 */
@Service
@Transactional(readOnly = true)
public class OpportunityScoreV2Service {

    private static final Logger log =
            LoggerFactory.getLogger(OpportunityScoreV2Service.class);

    private static final String POLICY_VERSION = "v2.1";

    private static final int FUNDAMENTAL_MAXIMUM = 100;
    private static final int TECHNICAL_MAXIMUM = 10;

    private final OpportunityScoreService opportunityService;
    private final TechnicalScoreService technicalService;
    private final CompanyRepository companyRepository;

    public OpportunityScoreV2Service(
            OpportunityScoreService opportunityService,
            TechnicalScoreService technicalService,
            CompanyRepository companyRepository
    ) {
        this.opportunityService = opportunityService;
        this.technicalService = technicalService;
        this.companyRepository = companyRepository;
    }

    public OpportunityScoreV2Response score(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        // A missing fundamental score is a genuine failure. It means the
        // company has insufficient financial history, so there is nothing
        // meaningful to report. That exception is allowed to propagate.
        int fundamentalScore =
                opportunityService.getScore(companyId);

        Integer technicalScore = null;
        String technicalNote = null;

        try {
            TechnicalScoreResponse technical =
                    technicalService.score(
                            company.getInstrument().getId()
                    );

            technicalScore = technical.technicalScore();

        } catch (RuntimeException ex) {

            technicalNote = describeTechnicalFailure(ex);

            log.info(
                    "Technical score unavailable for {}: {}",
                    company.getInstrument().getSymbol(),
                    technicalNote
            );
        }

        boolean technicalAvailable = technicalScore != null;

        int finalScore = technicalAvailable
                ? fundamentalScore + technicalScore
                : fundamentalScore;

        int maximumScore = technicalAvailable
                ? FUNDAMENTAL_MAXIMUM + TECHNICAL_MAXIMUM
                : FUNDAMENTAL_MAXIMUM;

        return new OpportunityScoreV2Response(
                company.getId(),
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                fundamentalScore,
                technicalScore,
                finalScore,
                maximumScore,
                determineRating(finalScore, maximumScore),
                POLICY_VERSION,
                technicalAvailable,
                technicalNote
        );
    }

    /**
     * Ratings are derived from the percentage of the achievable maximum
     * rather than an absolute score. Without this, a company scored on
     * fundamentals alone would be unfairly penalised against a company
     * that also earned technical points.
     */
    private String determineRating(
            int finalScore,
            int maximumScore
    ) {
        if (maximumScore <= 0) {
            return "UNRATED";
        }

        double percentage =
                (finalScore * 100.0) / maximumScore;

        if (percentage >= 90) {
            return "ELITE";
        }

        if (percentage >= 80) {
            return "STRONG";
        }

        if (percentage >= 65) {
            return "GOOD";
        }

        if (percentage >= 50) {
            return "MODERATE";
        }

        return "WEAK";
    }

    private String describeTechnicalFailure(RuntimeException ex) {

        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            return "Technical score unavailable";
        }

        if (message.toLowerCase().contains("price bar")) {
            return "Technical score unavailable because "
                    + "insufficient price history is stored";
        }

        return "Technical score unavailable: " + message;
    }
}
