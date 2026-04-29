package com.medsetu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * AI Chat Service using Groq's OpenAI-compatible API.
 * Free tier models: llama-3.1-8b-instant, llama3-8b-8192, mixtral-8x7b-32768
 * API Docs: https://console.groq.com/docs/openai
 */
@Service
public class AIChatService {

    private static final String SYSTEM_PROMPT =
            "You are Medsetu Health Assistant, a helpful medical information assistant. " +
            "Provide clear, concise general health information. " +
            "Always recommend consulting a qualified doctor for diagnosis, prescriptions, or medical decisions. " +
            "If you are uncertain about anything, say so clearly and suggest the user consult a doctor.";

    private static final List<String> ESCALATION_KEYWORDS = List.of(
            "consult a doctor", "i am not sure", "seek professional advice",
            "i cannot provide medical advice", "please consult", "see a doctor",
            "medical professional", "i'm not sure", "uncertain", "emergency",
            "immediately seek", "call 911", "go to the hospital"
    );

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AIChatService(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.model}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a message to Groq and returns the AI response text.
     * Uses the OpenAI-compatible /chat/completions endpoint.
     */
    public String sendMessage(String userMessage) {
        try {
            // Build request body in OpenAI chat completions format
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();

            // System message
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            // User message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq API error " + response.statusCode() + ": " + response.body());
                return "I'm currently experiencing technical difficulties. Please try again or consult a doctor directly.";
            }

            // Parse OpenAI-format response: choices[0].message.content
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return content.isBlank()
                    ? "I was unable to generate a response. Please try rephrasing your question."
                    : content;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Request was interrupted. Please try again.";
        } catch (Exception e) {
            System.err.println("AIChatService error: " + e.getMessage());
            return "I'm currently experiencing technical difficulties. Please try again or consult a doctor directly.";
        }
    }

    /**
     * Checks if the AI response warrants escalating to a human doctor agent.
     */
    public boolean isEscalationRequired(String aiResponse) {
        if (aiResponse == null) return false;
        String lowerResponse = aiResponse.toLowerCase();
        return ESCALATION_KEYWORDS.stream().anyMatch(lowerResponse::contains);
    }
}
