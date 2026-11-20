package com.freshmart.smartstock.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshmart.smartstock.config.AppProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls any OpenAI-compatible /chat/completions endpoint (OpenAI, Azure OpenAI, Groq, Ollama, ...).
 * The API key is injected from configuration (env / application-local.yml), never hard-coded.
 * Do NOT log the key or full prompts that contain business data.
 */
public class HttpLlmService implements LlmService {

    private final RestClient client;
    private final String model;

    public HttpLlmService(AppProperties.Llm config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(config.timeoutSeconds() > 0 ? config.timeoutSeconds() : 30);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.client = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                .build();
        this.model = config.model();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new LlmUnavailableException("Empty response from LLM provider", null);
            }
            String content = response.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new LlmUnavailableException("LLM returned no content", null);
            }
            return content;
        } catch (RestClientException e) {
            throw new LlmUnavailableException("LLM provider call failed: " + e.getClass().getSimpleName(), e);
        }
    }
}
