package com.abhiram.atlas.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IndianApiCompanyProfile(

        @JsonProperty("companyDescription")
        String companyDescription,

        @JsonProperty("mgIndustry")
        String industry,

        @JsonProperty("isInId")
        String isin
) {
}
