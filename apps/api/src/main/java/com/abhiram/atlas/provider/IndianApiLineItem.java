package com.abhiram.atlas.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single labelled financial figure.
 *
 * The key field has whitespace stripped by the provider, producing
 * identifiers such as "CashfromOperatingActivities". displayName
 * retains the spacing but is inconsistent, so lookups use key.
 *
 * value arrives as a string because the API returns formatted numbers
 * and occasional placeholder tokens.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndianApiLineItem(

        @JsonProperty("displayName")
        String displayName,

        @JsonProperty("key")
        String key,

        @JsonProperty("value")
        String value
) {
}
