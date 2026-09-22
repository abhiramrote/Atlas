package com.abhiram.atlas.provider;

import com.abhiram.atlas.config.IndianApiConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fundamentals provider for Indian listed companies.
 *
 * Response shape, confirmed from a live call:
 *
 *   financials[]
 *     FiscalYear, EndDate, Type ("Annual" | "Interim")
 *     stockFinancialMap
 *       INC[] { key, value }   income statement
 *       BAL[] { key, value }   balance sheet
 *       CAS[] { key, value }   cash flow
 *
 * Values are reported in crores. Atlas stores them as returned, so
 * ratios and growth are correct but absolute figures are in crores
 * rather than rupees. This is flagged rather than silently converted
 * because guessing the unit would be worse than documenting it.
 *
 * Every numeric parse failure resolves to null, never zero. A zero
 * revenue would make margins and growth produce confident nonsense.
 */
@Component
public class IndianApiProvider {

    private static final Logger log =
            LoggerFactory.getLogger(IndianApiProvider.class);

    public static final String PROVIDER_NAME = "INDIAN_API";

    // Income statement keys, in priority order.
    private static final List<String> REVENUE_KEYS = List.of(
            "Revenue",
            "TotalRevenue"
    );

    private static final List<String> NET_INCOME_KEYS = List.of(
            "NetIncome",
            "NetIncomeBeforeExtraItems",
            "NetIncomeAfterTaxes"
    );

    private static final List<String> OPERATING_INCOME_KEYS =
            List.of("OperatingIncome");

    // Cash flow statement keys.
    private static final List<String> OPERATING_CASH_FLOW_KEYS =
            List.of(
                    "CashfromOperatingActivities",
                    "CashFromOperatingActivities"
            );

    private final RestClient restClient;
    private final IndianApiConfig config;

    public IndianApiProvider(
            RestClient restClient,
            IndianApiConfig config
    ) {
        this.restClient = restClient;
        this.config = config;
    }

    public boolean isUsable() {
        return config.isUsable();
    }

    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public FundamentalsData fetchFundamentals(String symbol) {

        if (!config.isUsable()) {
            throw new IllegalStateException(
                    "IndianAPI provider is not configured. "
                            + "Set INDIANAPI_API_KEY and enable it."
            );
        }

        String normalized = symbol.trim().toUpperCase();

        String url = UriComponentsBuilder
                .fromUriString(config.getBaseUrl())
                .path("/stock")
                .queryParam("name", normalized)
                .build()
                .toUriString();

        IndianApiCompanyResponse response = restClient.get()
                .uri(url)
                .header("x-api-key", config.getApiKey())
                .retrieve()
                .body(IndianApiCompanyResponse.class);

        if (response == null) {
            throw new IllegalArgumentException(
                    "IndianAPI returned no body for " + normalized
            );
        }

        return toFundamentals(normalized, response);
    }

    private FundamentalsData toFundamentals(
            String symbol,
            IndianApiCompanyResponse response
    ) {
        List<FinancialPeriodData> periods =
                extractAnnualPeriods(response);

        String description = response.companyProfile() == null
                ? null
                : response.companyProfile().companyDescription();

        String industry = response.industry();

        if (isBlank(industry)
                && response.companyProfile() != null) {
            industry = response.companyProfile().industry();
        }

        // The endpoint exposes industry but not sector. Leaving this
        // null is honest; inventing a sector from the industry string
        // would create data that looks authoritative but is not.
        return new FundamentalsData(
                symbol,
                response.companyName(),
                null,
                industry,
                description,
                periods,
                PROVIDER_NAME
        );
    }

    /**
     * Extracts annual periods only.
     *
     * The live response mixes annual and interim rows in one list.
     * Including interim periods would make the growth engine compare
     * a quarter against a year, producing meaningless percentages.
     *
     * Results are sorted newest first because the growth and scoring
     * services expect that ordering.
     */
    List<FinancialPeriodData> extractAnnualPeriods(
            IndianApiCompanyResponse response
    ) {
        if (response.financials() == null) {
            return List.of();
        }

        List<FinancialPeriodData> periods = new ArrayList<>();

        for (IndianApiFinancialPeriod raw : response.financials()) {

            if (!raw.isAnnual()) {
                continue;
            }

            Integer fiscalYear = resolveFiscalYear(raw);

            if (fiscalYear == null) {
                log.debug(
                        "Skipping period with unresolvable year: {}",
                        raw.fiscalYear()
                );
                continue;
            }

            if (raw.stockFinancialMap() == null) {
                continue;
            }

            Map<String, String> income = indexItems(
                    raw.stockFinancialMap().incomeStatement()
            );

            Map<String, String> cashFlow = indexItems(
                    raw.stockFinancialMap().cashFlow()
            );

            FinancialPeriodData period = new FinancialPeriodData(
                    fiscalYear,
                    4,
                    lookup(income, REVENUE_KEYS),
                    lookup(income, NET_INCOME_KEYS),
                    lookup(income, OPERATING_INCOME_KEYS),
                    lookup(cashFlow, OPERATING_CASH_FLOW_KEYS)
            );

            if (period.isUsable()) {
                periods.add(period);
            }
        }

        periods.sort(
                Comparator.comparing(
                        FinancialPeriodData::fiscalYear
                ).reversed()
        );

        return periods;
    }

    /**
     * Builds a key to value index for one statement section.
     *
     * Duplicate keys keep the first occurrence, because the provider
     * lists primary figures before derived ones.
     */
    private Map<String, String> indexItems(
            List<IndianApiLineItem> items
    ) {
        Map<String, String> index = new HashMap<>();

        if (items == null) {
            return index;
        }

        for (IndianApiLineItem item : items) {

            if (item.key() == null || item.key().isBlank()) {
                continue;
            }

            index.putIfAbsent(item.key().trim(), item.value());
        }

        return index;
    }

    /**
     * Returns the first candidate key that yields a parseable value.
     *
     * Trying several keys matters because the provider is not fully
     * consistent. Some companies report Revenue, others only
     * TotalRevenue.
     */
    private BigDecimal lookup(
            Map<String, String> index,
            List<String> candidateKeys
    ) {
        for (String key : candidateKeys) {

            BigDecimal parsed = parseAmount(index.get(key));

            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private Integer resolveFiscalYear(
            IndianApiFinancialPeriod raw
    ) {
        Integer fromField = parseYear(raw.fiscalYear());

        if (fromField != null) {
            return fromField;
        }

        return parseYearFromDate(raw.endDate());
    }

    private Integer parseYear(String value) {

        if (isBlank(value)) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.length() >= 4) {

            String lastFour = cleaned.substring(
                    cleaned.length() - 4
            );

            try {
                int year = Integer.parseInt(lastFour);

                if (year >= 1900 && year <= 2200) {
                    return year;
                }

            } catch (NumberFormatException ignored) {
                // Fall through.
            }
        }

        return null;
    }

    private Integer parseYearFromDate(String value) {

        if (isBlank(value)) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.length() >= 10) {
            cleaned = cleaned.substring(0, 10);
        }

        try {
            return LocalDate.parse(cleaned).getYear();

        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Parses an Indian formatted numeric string.
     *
     * Handles thousands separators, currency decoration, accounting
     * negatives and placeholder tokens. Anything unrecognised becomes
     * null. Never zero.
     */
    BigDecimal parseAmount(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        String upper = cleaned.toUpperCase();

        if (upper.equals("-")
                || upper.equals("NA")
                || upper.equals("N/A")
                || upper.equals("NULL")
                || upper.equals("--")) {
            return null;
        }

        boolean negative = false;

        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            negative = true;
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        cleaned = cleaned
                .replace(",", "")
                .replace("\u20B9", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(cleaned);

            return negative ? parsed.negate() : parsed;

        } catch (NumberFormatException ex) {
            log.debug("Could not parse amount: {}", value);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
