package com.abhiram.atlas.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        UUID instrumentId,
        String symbol,
        String companyName,
        String exchange,
        String sector,
        String industry,
        String website,
        String description
) {
}