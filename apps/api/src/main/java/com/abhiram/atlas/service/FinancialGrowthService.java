package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.FinancialGrowthResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.entity.FinancialStatement;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;
import com.abhiram.atlas.repository.FinancialStatementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialGrowthService {

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private final CompanyRepository companyRepository;
    private final FinancialStatementRepository financialRepository;

    public FinancialGrowthService(
            CompanyRepository companyRepository,
            FinancialStatementRepository financialRepository
    ) {
        this.companyRepository = companyRepository;
        this.financialRepository = financialRepository;
    }

    public FinancialGrowthResponse getGrowth(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + companyId
                        )
                );

        List<FinancialStatement> statements =
                financialRepository
                        .findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(
                                companyId
                        );

        if (statements.size() < 2) {
            throw new ResourceNotFoundException(
                    "At least two financial periods are required "
                            + "for company: " + companyId
            );
        }

        FinancialStatement current = statements.get(0);
        FinancialStatement previous = statements.get(1);

        return new FinancialGrowthResponse(
                companyId,
                company.getInstrument().getSymbol(),
                current.getFiscalYear(),
                previous.getFiscalYear(),
                current.getRevenue(),
                previous.getRevenue(),
                calculateGrowth(
                        current.getRevenue(),
                        previous.getRevenue()
                ),
                calculateGrowth(
                        current.getNetIncome(),
                        previous.getNetIncome()
                ),
                calculateGrowth(
                        current.getOperatingProfit(),
                        previous.getOperatingProfit()
                ),
                calculateGrowth(
                        current.getOperatingCashFlow(),
                        previous.getOperatingCashFlow()
                )
        );
    }

    private BigDecimal calculateGrowth(
            BigDecimal currentValue,
            BigDecimal previousValue
    ) {
        if (currentValue == null || previousValue == null) {
            return null;
        }

        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return currentValue
                .subtract(previousValue)
                .divide(previousValue, 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}