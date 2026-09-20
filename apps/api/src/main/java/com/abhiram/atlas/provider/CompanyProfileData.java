package com.abhiram.atlas.provider;

public record CompanyProfileData(
        String symbol,
        String companyName,
        String exchange,
        String sector,
        String industry,
        String website,
        String description
) {
}