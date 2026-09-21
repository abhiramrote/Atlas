package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.ThesisEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThesisEventRepository
        extends JpaRepository<ThesisEvent, UUID> {

    List<ThesisEvent> findByThesisIdOrderByOccurredAtAsc(
            UUID thesisId
    );
}