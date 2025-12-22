package com.testing.automation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.dto.AiGenerateRequest;
import com.testing.automation.model.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are an expert QA Automation Engineer.
            Given an API definition (URL, Method, Headers, Body), generate a list of high-quality test cases.
            Each test case should include:
            - caseName: A descriptive name for the scenario.
            - method: The HTTP method.
            - url: The full URL.
            - headers: JSON string of headers.
            - body: JSON string of the request body.
            - assertionScript: A Groovy script for assertion. Use 'assert response.statusCode == 200' etc.
              The response object is available in the script context.

            Return ONLY a valid JSON array of objects.
            Include a mix of:
            - Happy path (Success)
            - Negative cases (Missing parameters, invalid types)
            - Boundary cases (Empty body, very long strings)
            """;

    public Mono<List<TestCase>> generateTestCases(AiGenerateRequest request) {
        String prompt = String.format(
                "%s\n\nAPI Definition:\nMethod: %s\nURL: %s\nHeaders: %s\nBody: %s\n\nGenerate %d cases.",
                SYSTEM_PROMPT,
                request.getMethod(),
                request.getUrl(),
                request.getHeaders(),
                request.getBody(),
                request.getCount());

        return aiService.generate(prompt)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, new TypeReference<List<TestCase>>() {
                        });
                    } catch (Exception e) {
                        log.error("Failed to parse AI generated test cases. JSON: {}", json, e);
                        return Collections.emptyList();
                    }
                });
    }

    public Mono<String> diagnoseFailure(String caseName, String executionLogs) {
        String prompt = String.format(
                "As an expert QA engineer, diagnose this test failure.\n" +
                        "Case Name: %s\n" +
                        "Execution Logs:\n%s\n\n" +
                        "Explain why it failed in plain language and suggest a specific fix.",
                caseName,
                executionLogs);

        return aiService.generate(prompt);
    }

    public Mono<String> suggestAssertions(String responseBody) {
        String prompt = String.format(
                "Based on this JSON response body, suggest 3-5 Groovy assertions for our testing platform.\n" +
                        "Response Body:\n%s\n\n" +
                        "Available variables in Groovy script:\n" +
                        "- status_code (int)\n" +
                        "- response (TestResponse object with getBody(), getHeaders(), getStatusCode())\n" +
                        "- json (parsed response body if JSON)\n\n" +
                        "Output ONLY the Groovy script code, one assertion per line.",
                responseBody);

        return aiService.generate(prompt);
    }

    public Mono<String> generateMockData(String url, String method, String description) {
        String prompt = String.format(
                "Generate a realistic JSON request body for the following API:\n" +
                        "URL: %s\n" +
                        "Method: %s\n" +
                        "Description: %s\n\n" +
                        "Output ONLY the JSON code, no other text. Use realistic data values.",
                url,
                method,
                description);

        return aiService.generate(prompt);
    }
}
