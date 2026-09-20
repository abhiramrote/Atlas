package com.abhiram.atlas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.OpportunityScoreResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

@Service
public class OpportunityScoreService {

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public OpportunityScoreService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository) {

        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    public OpportunityScoreResponse scoreCompany(
            UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found"));

        FinancialStatement fs =
                financialRepository.findByCompanyId(companyId)
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Financial statement not found"));

        BigDecimal revenueMargin =
                fs.getOperatingProfit()
                        .divide(fs.getRevenue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        BigDecimal profitMargin =
                fs.getNetIncome()
                        .divide(fs.getRevenue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        int score = 50;

        if (profitMargin.doubleValue() > 10) {
            score += 25;
        }

        if (revenueMargin.doubleValue() > 20) {
            score += 25;
        }

        String rating;

        if (score >= 90) {
            rating = "STRONG";
        } else if (score >= 70) {
            rating = "GOOD";
        } else {
            rating = "AVERAGE";
        }

        return new OpportunityScoreResponse(
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                profitMargin,
                revenueMargin,
                score,
                rating
        );
    }
}