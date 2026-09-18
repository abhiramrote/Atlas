package com.abhiram.atlas.dto;

import java.util.UUID;

public record InstrumentResponse(
        UUID id,
        String symbol,
        String companyName,
        String exchange,
        Boolean active
) {
}