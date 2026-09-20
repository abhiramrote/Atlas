package com.abhiram.atlas.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.OpportunityScoreV2Response;
import com.abhiram.atlas.dto.TechnicalScoreResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;

@Service
public class OpportunityScoreV2Service {

    private final OpportunityScoreService opportunityService;
    private final TechnicalScoreService technicalService;
    private final CompanyRepository companyRepository;

    public OpportunityScoreV2Service(
            OpportunityScoreService opportunityService,
            TechnicalScoreService technicalService,
            CompanyRepository companyRepository) {

        this.opportunityService = opportunityService;
        this.technicalService = technicalService;
        this.companyRepository = companyRepository;
    }

    public OpportunityScoreV2Response score(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found"));

        int fundamentalScore =
                opportunityService.getScore(companyId);

        TechnicalScoreResponse technical =
                technicalService.score(
                        company.getInstrument().getId());

        int finalScore =
                fundamentalScore +
                technical.technicalScore();

        String rating;

        if (finalScore >= 95) {
            rating = "ELITE";
        } else if (finalScore >= 85) {
            rating = "STRONG";
        } else if (finalScore >= 70) {
            rating = "GOOD";
        } else {
            rating = "AVERAGE";
        }

        return new OpportunityScoreV2Response(
                company.getInstrument().getSymbol(),
                fundamentalScore,
                technical.technicalScore(),
                finalScore,
                rating
        );
    }
}