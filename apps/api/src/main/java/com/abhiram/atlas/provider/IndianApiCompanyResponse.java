package com.abhiram.atlas.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Top level company payload.
 *
 * The live response has no sector field, only industry. Declaring a
 * sector field that never populates would create a permanently null
 * column that looks like missing data rather than absent data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndianApiCompanyResponse(

        @JsonProperty("companyName")
        String companyName,

        @JsonProperty("industry")
        String industry,

        @JsonProperty("companyProfile")
        IndianApiCompanyProfile companyProfile,

        @JsonProperty("currentPrice")
        Object currentPrice,

        @JsonProperty("financials")
        List<IndianApiFinancialPeriod> financials
) {
}