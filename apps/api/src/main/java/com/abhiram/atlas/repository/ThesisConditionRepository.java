package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.ThesisCondition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThesisConditionRepository
        extends JpaRepository<ThesisCondition, UUID> {

    List<ThesisCondition> findByThesisVersionId(
            UUID thesisVersionId
    );
}