package com.abhiram.atlas.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiram.atlas.entity.Instrument;

public interface InstrumentRepository
extends JpaRepository<Instrument, UUID> {
    
}
