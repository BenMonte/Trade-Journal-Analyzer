package com.ben.tradeanalyzer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends chat completion requests to the OpenAI API
 */
public class LlmClient {

    // Change this to switch the default model 
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public LlmClient(String apiKey) {
        this(apiKey, System.getenv().getOrDefault("OPENAI_MODEL", DEFAULT_MODEL));
    }

    public LlmClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.mapper = new ObjectMapper();
    }

    public String chatCompletion(String systemMessage, String userMessage) throws IOException {
        String requestBody = mapper.writeValueAsString(Map.of(
                "model", model,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemMessage),
                        Map.of("role", "user", "content", userMessage)
                )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LLM request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.at("/choices/0/message/content").asText();
    }
}
