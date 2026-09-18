package com.abhiram.atlas.service;

import com.abhiram.atlas.dto.CompanyResponse;
import com.abhiram.atlas.entity.Company;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.CompanyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    public List<CompanyResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CompanyResponse getById(UUID id) {
        Company company = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found: " + id
                        )
                );

        return toResponse(company);
    }

    public CompanyResponse getByInstrumentId(UUID instrumentId) {
        Company company = repository.findByInstrumentId(instrumentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Company not found for instrument: "
                                        + instrumentId
                        )
                );

        return toResponse(company);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getInstrument().getId(),
                company.getInstrument().getSymbol(),
                company.getInstrument().getCompanyName(),
                company.getInstrument().getExchange(),
                company.getSector(),
                company.getIndustry(),
                company.getWebsite(),
                company.getDescription()
        );
    }
}