package com.abhiram.atlas.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The three financial statements for a period.
 *
 * INC is the income statement, BAL the balance sheet and CAS the cash
 * flow statement. Each is a flat list of labelled line items rather
 * than a structured object, so values must be looked up by key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndianApiFinancialMap(

        @JsonProperty("INC")
        List<IndianApiLineItem> incomeStatement,

        @JsonProperty("BAL")
        List<IndianApiLineItem> balanceSheet,

        @JsonProperty("CAS")
        List<IndianApiLineItem> cashFlow
) {
}