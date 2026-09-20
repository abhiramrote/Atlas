package com.abhiram.atlas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwelveDataConfig {

    @Value("${twelvedata.api.key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}