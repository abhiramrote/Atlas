package com.abhiram.atlas.repository;

import com.abhiram.atlas.domain.ThesisState;
import com.abhiram.atlas.entity.Thesis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThesisRepository
        extends JpaRepository<Thesis, UUID> {

    List<Thesis> findByCompanyIdOrderByOpenedAtDesc(
            UUID companyId
    );

    List<Thesis> findByStateOrderByOpenedAtDesc(
            ThesisState state
    );

    List<Thesis> findByStateNotOrderByOpenedAtDesc(
            ThesisState state
    );
}
