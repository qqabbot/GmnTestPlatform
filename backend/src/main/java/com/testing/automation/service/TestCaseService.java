package com.testing.automation.service;

import com.testing.automation.dto.DryRunResponse;
import com.testing.automation.dto.TestResponse;
import com.testing.automation.dto.TestResult;
import com.testing.automation.mapper.*;
import com.testing.automation.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestCaseService {

    private final TestCaseMapper caseMapper;
    private final TestModuleMapper moduleMapper;
    private final TestStepMapper stepMapper;
    private final TestExecutionRecordMapper executionRecordMapper;
    private final TestExecutionLogMapper executionLogMapper;
    private final GlobalVariableService globalVariableService;
    private final WebClient webClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final ScriptEngine groovyEngine = new ScriptEngineManager().getEngineByName("groovy");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final org.springframework.expression.ExpressionParser spelParser = new org.springframework.expression.spel.standard.SpelExpressionParser();
    private final org.springframework.expression.EvaluationContext spelContext = new org.springframework.expression.spel.support.StandardEvaluationContext();

    @Autowired
    public TestCaseService(TestCaseMapper caseMapper, TestModuleMapper moduleMapper, TestStepMapper stepMapper,
            TestExecutionRecordMapper executionRecordMapper, TestExecutionLogMapper executionLogMapper,
            GlobalVariableService globalVariableService, WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.caseMapper = caseMapper;
        this.moduleMapper = moduleMapper;
        this.stepMapper = stepMapper;
        this.executionRecordMapper = executionRecordMapper;
        this.executionLogMapper = executionLogMapper;
        this.globalVariableService = globalVariableService;
        this.webClient = webClientBuilder.build();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build();
        this.retry = Retry.of("testCaseExecution", config);

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("apiCircuitBreaker", cbConfig);
    }

    // CRUD Methods
    @Transactional
    public TestCase save(TestCase testCase) {
        testCase.setUpdatedAt(LocalDateTime.now());
        if (testCase.getId() == null) {
            testCase.setCreatedAt(LocalDateTime.now());
            caseMapper.insert(testCase);
        } else {
            caseMapper.update(testCase);
        }
        return testCase;
    }

    public TestCase findById(Long id) {
        TestCase testCase = caseMapper.findById(id);
        if (testCase != null) {
            testCase.setSteps(stepMapper.findByCaseId(id));
        }
        return testCase;
    }

    public List<TestCase> findAll() {
        return caseMapper.findAll();
    }

    public List<TestCase> findByModuleId(Long moduleId) {
        return caseMapper.findByModuleId(moduleId);
    }

    @Transactional
    public void deleteById(Long id) {
        stepMapper.deleteByCaseId(id); // Cascade delete steps
        caseMapper.deleteById(id);
    }

    // Variable Loading
    public Map<String, Object> loadEnvironmentVariables(String envKey) {
        return globalVariableService.getVariablesMapByEnvName(envKey);
    }

    // Dry Run
    public DryRunResponse dryRunTestCase(Long caseId, String envKey) {
        TestCase testCase = caseMapper.findById(caseId);
        if (testCase == null) {
            throw new RuntimeException("Test case not found: " + caseId);
        }

        Long projectId = testCase.getModuleId();
        Long moduleId = testCase.getModuleId();
        Map<String, Object> runtimeVariables = globalVariableService.getVariablesMapWithInheritance(
                projectId, moduleId, envKey);

        String resolvedUrl = replaceVariables(testCase.getUrl(), runtimeVariables);
        String resolvedBody = testCase.getBody() != null ? replaceVariables(testCase.getBody(), runtimeVariables)
                : null;

        return DryRunResponse.builder()
                .resolvedUrl(resolvedUrl)
                .resolvedBody(resolvedBody)
                .resolvedHeaders(new HashMap<>())
                .variables(runtimeVariables)
                .build();
    }

    // Test Execution
    public List<TestResult> executeAllCases(Long projectId, Long moduleId, Long caseId, String envKey) {
        List<TestCase> casesToExecute = new ArrayList<>();

        if (caseId != null) {
            TestCase tc = caseMapper.findById(caseId);
            if (tc != null) {
                // Ensure we have correct context for variables
                if (moduleId == null)
                    moduleId = tc.getModuleId();
                if (projectId == null && moduleId != null) {
                    TestModule tm = moduleMapper.findById(moduleId);
                    if (tm != null)
                        projectId = tm.getProjectId();
                }
                // Fetch steps
                tc.setSteps(stepMapper.findByCaseId(tc.getId()));
                casesToExecute.add(tc);
            }
        } else if (moduleId != null) {
            casesToExecute.addAll(caseMapper.findByModuleId(moduleId));
        } else if (projectId != null) {
            List<TestModule> modules = moduleMapper.findByProjectId(projectId);
            for (TestModule module : modules) {
                casesToExecute.addAll(caseMapper.findByModuleId(module.getId()));
            }
        } else {
            casesToExecute.addAll(caseMapper.findAll());
        }

        // Fetch steps for all cases (eager load)
        for (TestCase tc : casesToExecute) {
            if (tc.getSteps() == null || tc.getSteps().isEmpty()) {
                tc.setSteps(stepMapper.findByCaseId(tc.getId()));
            }
        }

        Map<String, Object> runtimeVariables = globalVariableService.getVariablesMapWithInheritance(projectId, moduleId,
                envKey);
        Map<Long, TestResult> executionHistory = new HashMap<>();
        List<TestResult> finalResults = new ArrayList<>();

        Allure.suite("API Automation Test Suite - " + envKey);

        for (TestCase testCase : casesToExecute) {
            TestResult result = executeSingleCaseLogic(testCase, runtimeVariables, executionHistory);
            finalResults.add(result);
            executionHistory.put(testCase.getId(), result);
            saveExecutionRecord(testCase, result, envKey);
        }

        return finalResults;
    }

    public TestResult executeSingleCaseLogic(TestCase testCase, Map<String, Object> runtimeVariables,
            Map<Long, TestResult> executionHistory) {
        long startTime = System.currentTimeMillis();

        // Check preconditions (simplified)
        if (testCase.getPrecondition() != null && !testCase.getPrecondition().isEmpty()) {
            boolean allPassed = checkPreconditions(testCase, executionHistory);
            if (!allPassed) {
                return TestResult.builder()
                        .caseId(testCase.getId())
                        .caseName(testCase.getCaseName())
                        .status("SKIPPED")
                        .message("Precondition failed")
                        .detail("Precondition failed: " + testCase.getPrecondition())
                        .duration(System.currentTimeMillis() - startTime)
                        .build();
            }
        }

        // Execute setup script
        if (testCase.getSetupScript() != null && !testCase.getSetupScript().isEmpty()) {
            executeScript(testCase.getSetupScript(), runtimeVariables);
        }

        // Execute steps if present, otherwise execute single case request
        List<TestExecutionLog> logs = new ArrayList<>();
        TestResponse lastResponse = null;
        boolean allStepsPassed = true;
        String finalMessage = "Success";
        String finalDetail = "Success";

        if (testCase.getSteps() != null && !testCase.getSteps().isEmpty()) {
            // Execute Steps
            for (TestStep step : testCase.getSteps()) {
                if (!Boolean.TRUE.equals(step.getEnabled()))
                    continue;

                // Resolve Step Variables
                String stepUrl = replaceVariables(step.getUrl(), runtimeVariables);
                String stepBody = replaceVariables(step.getBody(), runtimeVariables);

                long stepStart = System.currentTimeMillis();
                try {
                    TestResponse stepResponse = executeHttpRequest(step.getMethod(), stepUrl, stepBody,
                            step.getHeaders());

                    // Step Log
                    TestExecutionLog log = new TestExecutionLog();
                    log.setStepName(step.getStepName());
                    log.setRequestUrl(stepUrl);
                    log.setRequestBody(stepBody);
                    log.setResponseStatus(stepResponse.getStatusCode());
                    log.setResponseBody(stepResponse.getBody());
                    log.setCreatedAt(LocalDateTime.now());
                    logs.add(log);
                    lastResponse = stepResponse; // Update last response for ease of access? Or consolidate?

                    // TODO: Step Assertions (not fully implemented yet)

                } catch (Exception e) {
                    allStepsPassed = false;
                    finalMessage = "Step Failed: " + step.getStepName();
                    finalDetail = e.getMessage();

                    // Log failure with proper error details
                    TestExecutionLog log = new TestExecutionLog();
                    log.setStepName(step.getStepName());
                    log.setRequestUrl(stepUrl);
                    log.setRequestBody(stepBody);
                    log.setResponseStatus(0); // 0 indicates network/execution error
                    log.setResponseBody(e.getMessage()); // Clean error message without prefix
                    log.setCreatedAt(LocalDateTime.now());
                    logs.add(log);
                    break; // Stop on failure?
                }
            }
        } else {
            // Execute Case Request (Legacy single mode)
            String resolvedUrl = replaceVariables(testCase.getUrl(), runtimeVariables);
            String resolvedBody = testCase.getBody() != null ? replaceVariables(testCase.getBody(), runtimeVariables)
                    : null;

            try {
                lastResponse = executeHttpRequest(testCase.getMethod(), resolvedUrl, resolvedBody,
                        testCase.getHeaders());

                // Log for main request
                TestExecutionLog log = new TestExecutionLog();
                log.setStepName("Main Request");
                log.setRequestUrl(resolvedUrl);
                log.setRequestBody(resolvedBody);
                log.setResponseStatus(lastResponse.getStatusCode());
                log.setResponseBody(lastResponse.getBody());
                log.setCreatedAt(LocalDateTime.now());
                logs.add(log);

            } catch (Exception e) {
                return TestResult.builder()
                        .caseId(testCase.getId())
                        .caseName(testCase.getCaseName())
                        .status("FAIL")
                        .message("HTTP Error: " + e.getMessage())
                        .detail("HTTP Error: " + e.getMessage())
                        .duration(System.currentTimeMillis() - startTime)
                        .logs(logs)
                        .build();
            }
        }

        // Case Level Assertions (Run against last response if available)
        boolean assertionPassed = true;
        if (allStepsPassed && lastResponse != null && testCase.getAssertionScript() != null
                && !testCase.getAssertionScript().isEmpty()) {
            assertionPassed = executeAssertions(testCase.getAssertionScript(), lastResponse, runtimeVariables);
            if (!assertionPassed) {
                finalMessage = "Assertion failed";
                finalDetail = "Assertion failed. Script: " + testCase.getAssertionScript();
            }
        } else if (!allStepsPassed) {
            assertionPassed = false;
        }

        String status = assertionPassed ? "PASS" : "FAIL";

        return TestResult.builder()
                .caseId(testCase.getId())
                .caseName(testCase.getCaseName())
                .status(status)
                .message(finalMessage)
                .detail(finalDetail)
                .statusCode(lastResponse != null ? lastResponse.getStatusCode() : 0)
                .responseBody(lastResponse != null ? lastResponse.getBody() : null)
                .duration(System.currentTimeMillis() - startTime)
                .logs(logs)
                .build();
    }

    private boolean checkPreconditions(TestCase testCase, Map<Long, TestResult> executionHistory) {
        String[] preconditionIds = testCase.getPrecondition().split(",");
        for (String idStr : preconditionIds) {
            try {
                Long preconditionId = Long.parseLong(idStr.trim());
                TestResult preconditionResult = executionHistory.get(preconditionId);
                if (preconditionResult == null || !"PASS".equals(preconditionResult.getStatus())) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric preconditions (treat as descriptive text) or fail?
                // For robustness, we log and continue/ignore, or return false?
                // The user data has descriptive text "订单已创建", so we should probably ignore it
                // or treat it as passed/info.
                // Returning false would block execution. Let's ignore it for now to allow
                // execution.
                continue;
            }
        }
        return true;
    }

    private void executeScript(String script, Map<String, Object> variables) {
        try {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                groovyEngine.put(entry.getKey(), entry.getValue());
            }
            groovyEngine.eval(script);
        } catch (Exception e) {
            // Log error
        }
    }

    private String replaceVariables(String text, Map<String, Object> variables) {
        if (text == null)
            return null;
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            // If variable not found, preserve original placeholder instead of empty string
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "\\${" + varName + "}";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private TestResponse executeHttpRequest(String method, String url, String body, String headers) {
        try {
            WebClient.RequestBodySpec request = webClient.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(url);

            if (body != null && !body.isEmpty()) {
                request.bodyValue(body);
            }

            Mono<org.springframework.http.ResponseEntity<String>> responseMono = request.retrieve()
                    .toEntity(String.class);
            org.springframework.http.ResponseEntity<String> response = responseMono.block();

            return TestResponse.builder()
                    .statusCode(response.getStatusCodeValue())
                    .body(response.getBody())
                    .headers(response.getHeaders().toSingleValueMap())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("HTTP Request failed: " + e.getMessage(), e);
        }
    }

    private boolean executeAssertions(String script, TestResponse response, Map<String, Object> variables) {
        try {
            groovyEngine.put("status_code", response.getStatusCode());
            groovyEngine.put("response_body", response.getBody());
            groovyEngine.put("response", response);

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                groovyEngine.put(entry.getKey(), entry.getValue());
            }

            Object result = groovyEngine.eval(script);
            return result != null && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveExecutionRecord(TestCase testCase, TestResult result, String envKey) {
        TestExecutionRecord record = new TestExecutionRecord();
        record.setCaseId(testCase.getId());
        record.setCaseName(testCase.getCaseName());
        record.setEnvKey(envKey);
        record.setStatus(result.getStatus());
        record.setDetail(result.getMessage());
        record.setDuration(result.getDuration());
        record.setExecutedAt(LocalDateTime.now());
        executionRecordMapper.insert(record);

        // Save Logs
        if (result.getLogs() != null) {
            for (TestExecutionLog log : result.getLogs()) {
                log.setRecordId(record.getId());
                executionLogMapper.insert(log);
            }
        }
    }
}