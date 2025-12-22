package com.testing.automation.controller;

import com.testing.automation.dto.AiGenerateRequest;
import com.testing.automation.model.TestCase;
import com.testing.automation.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiController {

    private final AiTaskService aiTaskService;

    @PostMapping("/generate-cases")
    public Mono<List<TestCase>> generateCases(@RequestBody AiGenerateRequest request) {
        return aiTaskService.generateTestCases(request);
    }

    @PostMapping("/diagnose")
    public Mono<String> diagnose(@RequestBody Map<String, String> request) {
        return aiTaskService.diagnoseFailure(request.get("caseName"), request.get("logs"));
    }

    @PostMapping("/suggest-assertions")
    public Mono<String> suggestAssertions(@RequestBody Map<String, String> request) {
        return aiTaskService.suggestAssertions(request.get("responseBody"));
    }

    @PostMapping("/generate-mock")
    public Mono<String> generateMock(@RequestBody Map<String, String> request) {
        return aiTaskService.generateMockData(request.get("url"), request.get("method"), request.get("description"));
    }
}
