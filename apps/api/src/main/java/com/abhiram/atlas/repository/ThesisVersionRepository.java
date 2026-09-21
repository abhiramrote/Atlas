package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.ThesisVersion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThesisVersionRepository
        extends JpaRepository<ThesisVersion, UUID> {

    List<ThesisVersion> findByThesisIdOrderByVersionNumberDesc(
            UUID thesisId
    );

    Optional<ThesisVersion> findByThesisIdAndVersionNumber(
            UUID thesisId,
            Integer versionNumber
    );

    Optional<ThesisVersion> findFirstByThesisIdOrderByVersionNumberDesc(
            UUID thesisId
    );
}