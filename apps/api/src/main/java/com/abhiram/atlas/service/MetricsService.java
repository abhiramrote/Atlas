package com.abhiram.atlas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.FinancialMetricsResponse;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.FinancialStatementRepository;

@Service
public class MetricsService {

    private final FinancialStatementRepository repository;

    public MetricsService(
            FinancialStatementRepository repository) {
        this.repository = repository;
    }

    public FinancialMetricsResponse getMetrics(
            UUID companyId) {

        FinancialStatement fs =
                repository.findByCompanyId(companyId)
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Financial statement not found"));

        BigDecimal revenueMargin =
                fs.getOperatingProfit()
                        .divide(fs.getRevenue(), 4,
                                RoundingMode.HALF_UP)
                        .multiply(
                                BigDecimal.valueOf(100));

        BigDecimal profitMargin =
                fs.getNetIncome()
                        .divide(fs.getRevenue(), 4,
                                RoundingMode.HALF_UP)
                        .multiply(
                                BigDecimal.valueOf(100));

        return new FinancialMetricsResponse(
                fs.getFiscalYear(),
                fs.getRevenue(),
                fs.getNetIncome(),
                fs.getOperatingProfit(),
                revenueMargin,
                profitMargin
        );
    }
}