package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository
        extends JpaRepository<Company, UUID> {

    Optional<Company> findByInstrumentId(UUID instrumentId);
}