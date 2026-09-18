package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.FinancialStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinancialStatementRepository
        extends JpaRepository<FinancialStatement, UUID> {

    List<FinancialStatement> findByCompanyId(UUID companyId);

}