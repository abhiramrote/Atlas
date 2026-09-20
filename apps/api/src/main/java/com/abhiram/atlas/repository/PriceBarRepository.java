package com.abhiram.atlas.repository;

import com.abhiram.atlas.entity.PriceBar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceBarRepository
        extends JpaRepository<PriceBar, UUID> {

    List<PriceBar>
    findByInstrumentIdOrderByTradeDateDesc(
            UUID instrumentId
    );
    void deleteByInstrumentId(UUID instrumentId);
}