package com.abhiram.atlas.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single reporting period.
 *
 * Type is either "Annual" or "Interim". The live INFY response
 * contains seven annual and ten interim periods in the same list, so
 * filtering on this field is mandatory. Mixing a quarter into an
 * annual series would make year on year growth meaningless.
 *
 * FiscalYear follows the Indian convention where FY2026 ends on
 * 31 March 2026.
 *
 * StatementDate is unreliable in the live data. Several periods share
 * the same StatementDate regardless of their actual EndDate, so it is
 * deliberately not used for year resolution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndianApiFinancialPeriod(

        @JsonProperty("FiscalYear")
        String fiscalYear,

        @JsonProperty("EndDate")
        String endDate,

        @JsonProperty("Type")
        String type,

        @JsonProperty("fiscalPeriodNumber")
        Integer fiscalPeriodNumber,

        @JsonProperty("stockFinancialMap")
        IndianApiFinancialMap stockFinancialMap
) {

    public boolean isAnnual() {
        return type != null
                && type.trim().equalsIgnoreCase("Annual");
    }
}