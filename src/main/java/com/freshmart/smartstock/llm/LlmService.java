package com.freshmart.smartstock.llm;

/**
 * Abstraction over the LLM provider (Java counterpart of ILlmService). The app depends on this interface only,
 * so tests and demos run with MockLlmService and no API key.
 */
public interface LlmService {

    /**
     * Sends a system + user prompt and returns the model's raw text answer (expected to be a JSON object).
     * Implementations must throw LlmUnavailableException on timeout / network / provider errors.
     */
    String complete(String systemPrompt, String userPrompt);

    /** true for the offline mock, so the API can tell the client the result is not from a real model. */
    default boolean isMock() {
        return false;
    }
}
