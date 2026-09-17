package com.abhiram.atlas.service;
import java.util.List;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.repository.InstrumentRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class InstrumentService {

    private final InstrumentRepository repository;

    public List<Instrument> getAll() {
        return repository.findAll();
    }
}
