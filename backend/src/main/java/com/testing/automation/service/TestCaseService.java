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
    private final ExtractorMapper extractorMapper;
    private final AssertionMapper assertionMapper;
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
            ExtractorMapper extractorMapper, AssertionMapper assertionMapper,
            GlobalVariableService globalVariableService, WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.caseMapper = caseMapper;
        this.moduleMapper = moduleMapper;
        this.stepMapper = stepMapper;
        this.executionRecordMapper = executionRecordMapper;
        this.executionLogMapper = executionLogMapper;
        this.extractorMapper = extractorMapper;
        this.assertionMapper = assertionMapper;
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
            // Load extractors and assertions for each step
            for (TestStep step : testCase.getSteps()) {
                if (step.getId() != null) {
                    step.setExtractors(extractorMapper.findByStepId(step.getId()));
                    step.setAssertions(assertionMapper.findByStepId(step.getId()));
                }
            }
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

        // Properly resolve projectId from moduleId
        Long moduleId = testCase.getModuleId();
        Long projectId = null;
        if (moduleId != null) {
            TestModule module = moduleMapper.findById(moduleId);
            if (module != null) {
                projectId = module.getProjectId();
            }
        }
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
                // Load extractors and assertions for each step
                for (TestStep step : tc.getSteps()) {
                    if (step.getId() != null) {
                        step.setExtractors(extractorMapper.findByStepId(step.getId()));
                        step.setAssertions(assertionMapper.findByStepId(step.getId()));
                    }
                }
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
            // Load extractors and assertions for each step
            for (TestStep step : tc.getSteps()) {
                if (step.getId() != null) {
                    step.setExtractors(extractorMapper.findByStepId(step.getId()));
                    step.setAssertions(assertionMapper.findByStepId(step.getId()));
                }
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
                    lastResponse = stepResponse;

                    // Execute Extractors to extract variables from response
                    if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                        for (Extractor extractor : step.getExtractors()) {
                            try {
                                Object extractedValue = executeExtractor(extractor, stepResponse);
                                if (extractedValue != null) {
                                    runtimeVariables.put(extractor.getVariableName(), extractedValue);
                                }
                            } catch (Exception e) {
                                // Log error but continue execution
                                System.err.println("Extractor failed for " + extractor.getVariableName() + ": " + e.getMessage());
                            }
                        }
                    }

                    // Execute Step Assertions
                    if (step.getAssertionScript() != null && !step.getAssertionScript().isEmpty()) {
                        boolean stepAssertionPassed = executeAssertions(step.getAssertionScript(), stepResponse, runtimeVariables);
                        if (!stepAssertionPassed) {
                            allStepsPassed = false;
                            finalMessage = "Step Assertion Failed: " + step.getStepName();
                            finalDetail = "Step assertion failed. Script: " + step.getAssertionScript();
                            break; // Stop execution on step assertion failure
                        }
                    }

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
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
            
            // Provide helper functions
            engine.put("vars", variables);
            engine.eval("def jsonPath(response, path) { " +
                    "try { " +
                    "  return com.jayway.jsonpath.JsonPath.read(response.getBody() ?: '{}', path); " +
                    "} catch (Exception e) { " +
                    "  return null; " +
                    "}" +
                    "}");
            
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
            engine.eval(script);
            
            // Update variables if modified in script
            variables.putAll((Map<String, Object>) engine.get("vars"));
        } catch (Exception e) {
            System.err.println("Script execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String replaceVariables(String text, Map<String, Object> variables) {
        if (text == null)
            return null;
        
        // Pattern to match ${...} expressions
        // Supports both simple variables ${varName} and SpEL expressions ${T(System).currentTimeMillis()}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String expression = matcher.group(1);
            String replacement;
            
            // Check if it's a SpEL expression (contains method calls, dots, parentheses)
            if (expression.contains("(") || expression.contains(".") || expression.startsWith("T(")) {
                try {
                    // Evaluate SpEL expression
                    Object result = spelParser.parseExpression(expression).getValue(spelContext);
                    replacement = result != null ? Matcher.quoteReplacement(result.toString()) : "\\${" + expression + "}";
                } catch (Exception e) {
                    // If SpEL evaluation fails, try as simple variable
                    Object value = variables.get(expression);
                    replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "\\${" + expression + "}";
                }
            } else {
                // Simple variable replacement
                Object value = variables.get(expression);
                replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "\\${" + expression + "}";
            }
            
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private TestResponse executeHttpRequest(String method, String url, String body, String headers) {
        try {
            // Apply Resilience4j decorators
            return Retry.decorateSupplier(retry, () ->
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    WebClient.RequestBodySpec request = webClient.method(org.springframework.http.HttpMethod.valueOf(method))
                            .uri(url);

                    // Parse and apply headers
                    if (headers != null && !headers.trim().isEmpty()) {
                        try {
                            Map<String, String> headerMap = objectMapper.readValue(headers, 
                                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
                            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                                request.header(entry.getKey(), entry.getValue());
                            }
                        } catch (Exception e) {
                            // If JSON parsing fails, try as simple key-value pairs
                            System.err.println("Failed to parse headers as JSON: " + e.getMessage());
                        }
                    }

                    if (body != null && !body.isEmpty()) {
                        request.bodyValue(body);
                    }

                    Mono<org.springframework.http.ResponseEntity<String>> responseMono = request.retrieve()
                            .toEntity(String.class);
                    org.springframework.http.ResponseEntity<String> response = responseMono.block();

                    return TestResponse.builder()
                            .statusCode(response.getStatusCode().value()) // Use non-deprecated API
                            .body(response.getBody())
                            .headers(response.getHeaders().toSingleValueMap())
                            .build();
                })
            ).get();
        } catch (Exception e) {
            throw new RuntimeException("HTTP Request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute extractor to extract value from response
     */
    private Object executeExtractor(Extractor extractor, TestResponse response) {
        try {
            String type = extractor.getType() != null ? extractor.getType().toUpperCase() : "JSONPATH";
            String expression = extractor.getExpression();
            
            switch (type) {
                case "JSONPATH":
                    if (response.getBody() != null) {
                        return JsonPath.read(response.getBody(), expression);
                    }
                    break;
                case "HEADER":
                    if (response.getHeaders() != null && response.getHeaders().containsKey(expression)) {
                        return response.getHeaders().get(expression);
                    }
                    break;
                case "REGEX":
                    if (response.getBody() != null) {
                        Pattern pattern = Pattern.compile(expression);
                        Matcher matcher = pattern.matcher(response.getBody());
                        if (matcher.find() && matcher.groupCount() > 0) {
                            return matcher.group(1);
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown extractor type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Extractor execution failed: " + e.getMessage());
            throw new RuntimeException("Extractor failed: " + e.getMessage(), e);
        }
        return null;
    }

    private boolean executeAssertions(String script, TestResponse response, Map<String, Object> variables) {
        try {
            // Create a new script engine instance for each execution to avoid state pollution
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
            
            // Provide helper functions and objects
            engine.put("status_code", response.getStatusCode());
            engine.put("response_body", response.getBody());
            engine.put("response", response);
            engine.put("headers", response.getHeaders() != null ? response.getHeaders() : new HashMap<>());
            
            // Provide vars object for variable manipulation
            Map<String, Object> vars = new HashMap<>(variables);
            engine.put("vars", vars);
            
            // Provide jsonPath helper function
            engine.eval("def jsonPath(response, path) { " +
                    "try { " +
                    "  return com.jayway.jsonpath.JsonPath.read(response.getBody() ?: '{}', path); " +
                    "} catch (Exception e) { " +
                    "  return null; " +
                    "}" +
                    "}");
            
            // Add all runtime variables to script context
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }

            Object result = engine.eval(script);
            
            // Update variables from vars object if modified
            variables.putAll(vars);
            
            return result != null && (Boolean) result;
        } catch (Exception e) {
            System.err.println("Assertion execution failed: " + e.getMessage());
            e.printStackTrace();
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