package com.freshmart.smartstock.llm;

/**
 * Offline stand-in used when no API key is configured.
 * TODO[Member E]: make it return a deterministic, realistic JSON for the 8 sample invoices (e.g. match product
 * names found in the user prompt) so the demo and unit tests do not depend on a network or a paid key.
 */
public class MockLlmService implements LlmService {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "{\"supplierName\": null, \"items\": [], \"unmatched\": []}";
    }

    @Override
    public boolean isMock() {
        return true;
    }
}
