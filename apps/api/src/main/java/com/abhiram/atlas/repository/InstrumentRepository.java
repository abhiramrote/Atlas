package com.abhiram.atlas.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiram.atlas.entity.Instrument;
import java.util.Optional;
public interface InstrumentRepository
extends JpaRepository<Instrument, UUID> {
    Optional<Instrument> findBySymbol(String symbol);
}
