package com.testing.automation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.url}")
    private String apiUrl;

    public AiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 Gemini API 生成内容
     */
    public Mono<String> generate(String prompt) {
        if ("your_api_key_here".equals(apiKey) || apiKey == null || apiKey.isEmpty()) {
            return Mono.error(new RuntimeException(
                    "GEMINI_API_KEY is not configured. Please set it in application.yml or environment variables."));
        }

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        /*
         * // For v1 version, the field name is response_mime_type, but let's remove it
         * for now to verify basic connectivity
         * Map<String, Object> generationConfig = new HashMap<>();
         * generationConfig.put("response_mime_type", "application/json");
         * body.put("generationConfig", generationConfig);
         */

        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(JsonNode.class)
                        .flatMap(errorBody -> {
                            String message = errorBody.path("error").path("message").asText();
                            return Mono.error(new RuntimeException(message));
                        }))
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    try {
                        return response.path("candidates")
                                .get(0)
                                .path("content")
                                .path("parts")
                                .get(0)
                                .path("text")
                                .asText();
                    } catch (Exception e) {
                        log.error("Failed to parse Gemini response: {}", response, e);
                        throw new RuntimeException("AI response format error");
                    }
                })
                .doOnError(e -> log.error("Gemini API call failed", e));
    }
}
