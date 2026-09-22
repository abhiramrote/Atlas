package com.abhiram.atlas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration for the IndianAPI market data provider.
 *
 * The key is supplied through an environment variable so it never
 * enters source control. Atlas must still start when the key is
 * absent, because the provider is optional and the rest of the
 * application should remain usable without it.
 */
@Component
public class IndianApiConfig {

    @Value("${indianapi.api.key:}")
    private String apiKey;

    @Value("${indianapi.base.url:https://stock.indianapi.in}")
    private String baseUrl;

    @Value("${indianapi.enabled:false}")
    private boolean enabled;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * The provider is usable only when it is explicitly enabled and a
     * key is present. Checking both prevents confusing failures where
     * the flag is on but the credential was never exported.
     */
    public boolean isUsable() {
        return enabled
                && apiKey != null
                && !apiKey.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
