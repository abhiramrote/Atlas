package com.abhiram.atlas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.ResearchSummaryResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

@Service
public class ResearchService {

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public ResearchService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository) {

        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    public ResearchSummaryResponse getSummary(UUID companyId) {

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
                        .divide(fs.getRevenue(), 4,
                                RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        BigDecimal profitMargin =
                fs.getNetIncome()
                        .divide(fs.getRevenue(), 4,
                                RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return new ResearchSummaryResponse(
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                company.getSector(),

                fs.getRevenue(),
                fs.getNetIncome(),

                revenueMargin,
                profitMargin
        );
    }
}