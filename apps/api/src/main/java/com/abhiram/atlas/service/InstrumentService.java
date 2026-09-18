package com.abhiram.atlas.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.abhiram.atlas.dto.InstrumentResponse;
import com.abhiram.atlas.entity.Instrument;
import com.abhiram.atlas.exception.ResourceNotFoundException;
import com.abhiram.atlas.repository.InstrumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository repository;

    public List<InstrumentResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(i -> new InstrumentResponse(
                        i.getId(),
                        i.getSymbol(),
                        i.getCompanyName(),
                        i.getExchange(),
                        i.getActive()
                ))
                .toList();
    }

    public InstrumentResponse getById(UUID id) {

        Instrument instrument = repository.findById(id)
                .orElseThrow(() ->
        new ResourceNotFoundException(
                "Instrument not found: " + id));

        return new InstrumentResponse(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getCompanyName(),
                instrument.getExchange(),
                instrument.getActive()
        );
    }
    public InstrumentResponse getBySymbol(String symbol) {

        Instrument instrument = repository.findBySymbol(symbol)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                            "Instrument not found: " + symbol));

                        return new InstrumentResponse(
                                instrument.getId(),
                                instrument.getSymbol(),
                                instrument.getCompanyName(),
                                instrument.getExchange(),
                                instrument.getActive()
                        );
        }
}