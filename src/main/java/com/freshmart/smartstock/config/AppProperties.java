package com.freshmart.smartstock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for the `app.*` block of application.yml.
 * Secrets (jwt.secret, llm.apiKey) come from environment variables / application-local.yml, never from Git.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, Jwt jwt, Llm llm, Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Llm(String apiKey, String baseUrl, String model, int timeoutSeconds) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Seed(boolean enabled, String defaultPassword) {
    }
}
