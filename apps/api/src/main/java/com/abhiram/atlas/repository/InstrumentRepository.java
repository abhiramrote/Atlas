package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.Instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository
        extends JpaRepository<Instrument, UUID> {

    Optional<Instrument> findBySymbol(String symbol);

    Optional<Instrument> findBySymbolIgnoreCase(String symbol);
}