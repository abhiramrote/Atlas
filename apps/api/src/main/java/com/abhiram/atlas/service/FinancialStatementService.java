package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.FinancialStatementResponse;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FinancialStatementService {

    private final FinancialStatementRepository repository;

    public FinancialStatementService(
            FinancialStatementRepository repository) {
        this.repository = repository;
    }

    public List<FinancialStatementResponse>
    getByCompanyId(UUID companyId) {

        return repository.findByCompanyId(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FinancialStatementResponse toResponse(
            FinancialStatement fs) {

        return new FinancialStatementResponse(
                fs.getFiscalYear(),
                fs.getFiscalQuarter(),
                fs.getRevenue(),
                fs.getNetIncome(),
                fs.getOperatingProfit(),
                fs.getOperatingCashFlow()
        );
    }
}