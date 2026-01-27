package com.testing.automation.service;

import com.testing.automation.dto.DryRunResponse;
import com.testing.automation.dto.TestResponse;
import com.testing.automation.dto.TestResult;
import com.testing.automation.Mapper.*;
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
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
        // Extract moduleId from nested module object if present
        if (testCase.getModuleId() == null && testCase.getModule() != null && testCase.getModule().getId() != null) {
            testCase.setModuleId(testCase.getModule().getId());
        }

        // Validate moduleId is present
        if (testCase.getModuleId() == null) {
            throw new IllegalArgumentException("moduleId is required and cannot be null");
        }

        testCase.setUpdatedAt(LocalDateTime.now());
        boolean isNew = testCase.getId() == null;
        if (isNew) {
            testCase.setCreatedAt(LocalDateTime.now());
            caseMapper.insert(testCase);
        } else {
            caseMapper.update(testCase);
            // Delete existing steps for update case (we'll re-add them)
            stepMapper.deleteByCaseId(testCase.getId());
        }

        // Save steps, extractors, and assertions
        List<TestStep> effectiveSteps = testCase.getSteps();
        if (effectiveSteps != null && !effectiveSteps.isEmpty()) {
            for (TestStep step : effectiveSteps) {
                step.setCaseId(testCase.getId());
                step.setUpdatedAt(LocalDateTime.now());
                if (step.getId() == null) {
                    step.setCreatedAt(LocalDateTime.now());
                }
                stepMapper.insert(step);

                // Save extractors
                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                    for (com.testing.automation.model.Extractor extractor : step.getExtractors()) {
                        String varName = extractor.getVariableName();
                        String expression = extractor.getExpression();

                        // Skip if variable name or expression is missing (invalid/empty extractor)
                        if (varName == null || varName.trim().isEmpty() || expression == null
                                || expression.trim().isEmpty()) {
                            continue;
                        }

                        extractor.setStepId(step.getId());
                        // Normalize variable name: remove ${} wrapper or $ prefix if present
                        varName = varName.trim();
                        // Remove ${} wrapper: ${token} -> token
                        if (varName.startsWith("${") && varName.endsWith("}")) {
                            varName = varName.substring(2, varName.length() - 1);
                        }
                        // Remove $ prefix: $token -> token
                        else if (varName.startsWith("$")) {
                            varName = varName.substring(1);
                        }
                        extractor.setVariableName(varName);

                        // Set type based on source if not set
                        if (extractor.getType() == null || extractor.getType().isEmpty()) {
                            extractor.setType("JSONPATH");
                        }
                        extractor.setCreatedAt(LocalDateTime.now());
                        extractorMapper.insert(extractor);
                    }
                }

                // Save assertions
                if (step.getAssertions() != null && !step.getAssertions().isEmpty()) {
                    for (com.testing.automation.model.Assertion assertion : step.getAssertions()) {
                        // Skip if expression and expected value are both missing
                        if ((assertion.getExpression() == null || assertion.getExpression().trim().isEmpty()) &&
                                (assertion.getExpectedValue() == null
                                        || assertion.getExpectedValue().trim().isEmpty())) {
                            continue;
                        }

                        assertion.setStepId(step.getId());
                        assertion.setCreatedAt(LocalDateTime.now());
                        // Ensure type is not null (database constraint)
                        if (assertion.getType() == null || assertion.getType().trim().isEmpty()) {
                            assertion.setType("script");
                        }
                        assertionMapper.insert(assertion);
                    }
                }
            }
        }

        return testCase;
    }

    public TestCase findById(Long id) {
        return caseMapper.findByIdWithDetails(id);
    }

    public List<TestCase> findAll() {
        return caseMapper.findAll();
    }

    public List<TestCase> findByModuleId(Long moduleId) {
        return caseMapper.findByModuleId(moduleId);
    }

    /**
     * 按项目ID分页查询用例
     * 
     * @param projectId 项目ID
     * @param page      页码（从0开始）
     * @param size      每页大小
     * @param keyword   搜索关键词（可选）
     * @return 包含 cases 列表和 total 总数的 Map
     */
    public Map<String, Object> findByProjectIdWithPagination(Long projectId, int page, int size, String keyword) {
        // 获取项目下的所有模块
        List<TestModule> modules = moduleMapper.findByProjectId(projectId);
        List<Long> moduleIds = new ArrayList<>();
        for (TestModule module : modules) {
            moduleIds.add(module.getId());
        }

        // 如果没有模块，返回空结果
        if (moduleIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("cases", new ArrayList<>());
            result.put("total", 0);
            result.put("page", page);
            result.put("size", size);
            return result;
        }

        // 计算偏移量
        int offset = page * size;

        // 查询总数
        int total = caseMapper.countByModuleIds(moduleIds, keyword);

        // 查询分页数据
        List<TestCase> cases = caseMapper.findByModuleIdsWithPagination(moduleIds, keyword, offset, size);

        Map<String, Object> result = new HashMap<>();
        result.put("cases", cases);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
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
        TestCase testCase = caseMapper.findByIdWithDetails(caseId);
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

        // Execute setup script during dry run if present
        if (testCase.getSetupScript() != null && !testCase.getSetupScript().isEmpty()) {
            System.out.println("Executing Pre-request Script in Dry Run for case: " + testCase.getCaseName());
            executeScript(testCase.getSetupScript(), runtimeVariables);
        }

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
            TestCase tc = caseMapper.findByIdWithDetails(caseId);
            if (tc != null) {
                // Ensure we have correct context for variables
                if (moduleId == null)
                    moduleId = tc.getModuleId();
                if (projectId == null && moduleId != null) {
                    TestModule tm = moduleMapper.findById(moduleId);
                    if (tm != null)
                        projectId = tm.getProjectId();
                }
                casesToExecute.add(tc);
            }
        } else if (moduleId != null) {
            casesToExecute.addAll(caseMapper.findByModuleIdWithDetails(moduleId));
        } else if (projectId != null) {
            List<TestModule> modules = moduleMapper.findByProjectId(projectId);
            for (TestModule module : modules) {
                casesToExecute.addAll(caseMapper.findByModuleIdWithDetails(module.getId()));
            }
        } else {
            casesToExecute.addAll(caseMapper.findAllWithDetails());
        }

        // Steps, extractors, and assertions are now eagerly loaded by the mapper
        // No need for manual iteration here

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

    private static final int MAX_RECURSION_DEPTH = 10;

    public TestResult executeSingleCaseLogic(TestCase testCase, Map<String, Object> runtimeVariables,
            Map<Long, TestResult> executionHistory) {
        return executeSingleCaseLogic(testCase, runtimeVariables, executionHistory, 0);
    }

    public TestResult executeSingleCaseLogic(TestCase testCase, Map<String, Object> runtimeVariables,
            Map<Long, TestResult> executionHistory, int currentDepth) {
        if (currentDepth > MAX_RECURSION_DEPTH) {
            return TestResult.builder()
                    .caseId(testCase.getId())
                    .caseName(testCase.getCaseName())
                    .status("FAIL")
                    .message("Max recursion depth exceeded")
                    .detail("Circular dependency detected or nesting too deep (Max: " + MAX_RECURSION_DEPTH + ")")
                    .duration(0L)
                    .build();
        }

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

        List<TestStep> effectiveSteps = testCase.getSteps();
        if (effectiveSteps != null && !effectiveSteps.isEmpty()) {
            // Execute Steps
            for (TestStep step : effectiveSteps) {
                if (!Boolean.TRUE.equals(step.getEnabled()))
                    continue;

                long stepStart = System.currentTimeMillis();
                TestResponse stepResponse = null;
                String stepUrl = null;
                String stepBody = null;

                try {
                    // Check if step references another test case
                    if (step.getReferenceCaseId() != null) {
                        // Execute referenced test case
                        TestCase referencedCase = caseMapper.findById(step.getReferenceCaseId());
                        if (referencedCase == null) {
                            throw new RuntimeException("Referenced test case not found: " + step.getReferenceCaseId());
                        }

                        // Load steps for referenced case
                        referencedCase.setSteps(stepMapper.findByCaseId(referencedCase.getId()));
                        // Load extractors and assertions
                        for (TestStep refStep : referencedCase.getSteps()) {
                            if (refStep.getId() != null) {
                                refStep.setExtractors(extractorMapper.findByStepId(refStep.getId()));
                                refStep.setAssertions(assertionMapper.findByStepId(refStep.getId()));
                            }
                        }

                        // Execute referenced case (recursive with depth check)
                        TestResult referencedResult = executeSingleCaseLogic(referencedCase, runtimeVariables,
                                executionHistory, currentDepth + 1);

                        // Use the last response from referenced case
                        Integer statusCode = referencedResult.getStatusCode();
                        if ("FAIL".equals(referencedResult.getStatus())
                                && referencedResult.getMessage().contains("Max recursion depth")) {
                            throw new RuntimeException(referencedResult.getMessage());
                        }

                        if (statusCode != null && statusCode > 0) {
                            stepResponse = TestResponse.builder()
                                    .statusCode(statusCode)
                                    .body(referencedResult.getResponseBody())
                                    .headers(new HashMap<>())
                                    .build();
                            stepUrl = "Referenced Case: " + referencedCase.getCaseName() + " (ID: "
                                    + step.getReferenceCaseId() + ")";
                            stepBody = "Executed referenced test case";

                            // If referenced case failed, mark step as failed
                            if (!"PASS".equals(referencedResult.getStatus())) {
                                // We still capture the response for logging, but fail the step
                                allStepsPassed = false;
                                finalMessage = "Step Failed: Referenced Case Failed";
                                finalDetail = "Referenced execution status: " + referencedResult.getStatus()
                                        + ", Message: " + referencedResult.getMessage();
                            }
                        } else {
                            throw new RuntimeException(
                                    "Referenced case execution failed: " + referencedResult.getMessage());
                        }
                    } else {
                        // Normal step execution
                        // Resolve Step Variables (including variables extracted from previous steps)
                        stepUrl = replaceVariables(step.getUrl(), runtimeVariables);
                        stepBody = replaceVariables(step.getBody(), runtimeVariables);
                        // Also replace variables in headers
                        String resolvedHeaders = step.getHeaders();
                        if (resolvedHeaders != null && !resolvedHeaders.trim().isEmpty()) {
                            resolvedHeaders = replaceVariables(resolvedHeaders, runtimeVariables);
                        }

                        stepResponse = executeHttpRequest(step.getMethod(), stepUrl, stepBody,
                                resolvedHeaders);
                    }

                    // Step Log
                    TestExecutionLog log = new TestExecutionLog();
                    log.setStepName(step.getStepName());
                    log.setRequestUrl(stepUrl);
                    log.setRequestBody(stepBody);
                    // Set request headers (use resolved headers for display)
                    String stepResolvedHeaders = step.getHeaders();
                    if (stepResolvedHeaders != null && !stepResolvedHeaders.trim().isEmpty()) {
                        stepResolvedHeaders = replaceVariables(stepResolvedHeaders, runtimeVariables);
                    }
                    log.setRequestHeaders(stepResolvedHeaders != null ? stepResolvedHeaders : "{}");
                    // Set response headers
                    if (stepResponse.getHeaders() != null && !stepResponse.getHeaders().isEmpty()) {
                        try {
                            log.setResponseHeaders(objectMapper.writeValueAsString(stepResponse.getHeaders()));
                        } catch (Exception e) {
                            log.setResponseHeaders("{}");
                        }
                    } else {
                        log.setResponseHeaders("{}");
                    }
                    log.setResponseStatus(stepResponse.getStatusCode());
                    log.setResponseBody(stepResponse.getBody());
                    // Save variable snapshot for this step
                    try {
                        log.setVariableSnapshot(objectMapper.writeValueAsString(runtimeVariables));
                    } catch (Exception e) {
                        log.setVariableSnapshot("{}");
                    }
                    log.setCreatedAt(LocalDateTime.now());
                    logs.add(log);
                    lastResponse = stepResponse;

                    // Execute Extractors to extract variables from response
                    if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                        for (Extractor extractor : step.getExtractors()) {
                            // executeExtractor now returns null instead of throwing for path not found
                            // errors
                            Object extractedValue = executeExtractor(extractor, stepResponse);

                            // Normalize variable name: remove ${} wrapper or $ prefix if present
                            String varName = extractor.getVariableName();
                            if (varName != null && !varName.isEmpty()) {
                                varName = varName.trim();
                                // Remove ${} wrapper: ${token} -> token
                                if (varName.startsWith("${") && varName.endsWith("}")) {
                                    varName = varName.substring(2, varName.length() - 1);
                                }
                                // Remove $ prefix: $token -> token
                                else if (varName.startsWith("$")) {
                                    varName = varName.substring(1);
                                }

                                if (extractedValue != null) {
                                    runtimeVariables.put(varName, extractedValue);
                                    System.out.println("Extracted variable: " + varName + " = " + extractedValue);
                                } else {
                                    // Log warning but don't fail the test
                                    System.out.println("Warning: Extractor for variable '" + varName
                                            + "' returned null (path not found in response)");
                                }
                            }
                        }
                    }

                    // Execute Step Assertions
                    if (step.getAssertionScript() != null && !step.getAssertionScript().isEmpty()) {
                        boolean stepAssertionPassed = executeAssertions(step.getAssertionScript(), stepResponse,
                                runtimeVariables);
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
                    log.setRequestUrl(stepUrl != null ? stepUrl : step.getUrl());
                    log.setRequestBody(stepBody != null ? stepBody : step.getBody());
                    // Set request headers even on error
                    log.setRequestHeaders(step.getHeaders() != null ? step.getHeaders() : "{}");
                    log.setResponseHeaders("{}");
                    log.setResponseStatus(0); // 0 indicates network/execution error
                    log.setResponseBody(e.getMessage()); // Clean error message without prefix
                    // Save variable snapshot even on error
                    try {
                        log.setVariableSnapshot(objectMapper.writeValueAsString(runtimeVariables));
                    } catch (Exception ex) {
                        log.setVariableSnapshot("{}");
                    }
                    log.setCreatedAt(LocalDateTime.now());
                    logs.add(log);
                    break; // Stop on failure?
                }
            }
        }

        // Execute Main Request if URL and method are provided
        // This executes after steps (if any) so that variables extracted from steps can
        // be used
        if (testCase.getUrl() != null && !testCase.getUrl().trim().isEmpty()
                && testCase.getMethod() != null && !testCase.getMethod().trim().isEmpty()) {
            try {
                // Resolve variables (including those extracted from steps)
                String resolvedUrl = replaceVariables(testCase.getUrl(), runtimeVariables);
                String resolvedBody = testCase.getBody() != null
                        ? replaceVariables(testCase.getBody(), runtimeVariables)
                        : null;
                // Also replace variables in headers (including variables from steps)
                String resolvedHeaders = testCase.getHeaders();
                if (resolvedHeaders != null && !resolvedHeaders.trim().isEmpty()) {
                    resolvedHeaders = replaceVariables(resolvedHeaders, runtimeVariables);
                }

                lastResponse = executeHttpRequest(testCase.getMethod(), resolvedUrl, resolvedBody,
                        resolvedHeaders);

                // Log for main request
                TestExecutionLog log = new TestExecutionLog();
                log.setStepName("Main Request");
                log.setRequestUrl(resolvedUrl);
                log.setRequestBody(resolvedBody);
                // Set request headers (use resolved headers for display, but log original for
                // reference)
                log.setRequestHeaders(resolvedHeaders != null ? resolvedHeaders
                        : (testCase.getHeaders() != null ? testCase.getHeaders() : "{}"));
                // Set response headers
                if (lastResponse.getHeaders() != null && !lastResponse.getHeaders().isEmpty()) {
                    try {
                        log.setResponseHeaders(objectMapper.writeValueAsString(lastResponse.getHeaders()));
                    } catch (Exception e) {
                        log.setResponseHeaders("{}");
                    }
                } else {
                    log.setResponseHeaders("{}");
                }
                log.setResponseStatus(lastResponse.getStatusCode());
                log.setResponseBody(lastResponse.getBody());
                // Save variable snapshot
                try {
                    log.setVariableSnapshot(objectMapper.writeValueAsString(runtimeVariables));
                } catch (Exception e) {
                    log.setVariableSnapshot("{}");
                }
                log.setCreatedAt(LocalDateTime.now());
                logs.add(log);

            } catch (Exception e) {
                // If main request fails, mark as failed but don't return early if steps passed
                allStepsPassed = false;
                finalMessage = "Main Request Failed";
                finalDetail = "HTTP Error: " + e.getMessage();

                // Log failure
                TestExecutionLog log = new TestExecutionLog();
                log.setStepName("Main Request");
                log.setRequestUrl(testCase.getUrl());
                log.setRequestBody(testCase.getBody());
                log.setRequestHeaders(testCase.getHeaders() != null ? testCase.getHeaders() : "{}");
                log.setResponseHeaders("{}");
                log.setResponseStatus(0);
                log.setResponseBody(e.getMessage());
                try {
                    log.setVariableSnapshot(objectMapper.writeValueAsString(runtimeVariables));
                } catch (Exception ex) {
                    log.setVariableSnapshot("{}");
                }
                log.setCreatedAt(LocalDateTime.now());
                logs.add(log);
            }
        }

        // Case Level Assertions (Run against last response if available)
        boolean assertionPassed = true;
        if (allStepsPassed && lastResponse != null && testCase.getEffectiveAssertionScript() != null
                && !testCase.getEffectiveAssertionScript().isEmpty()) {
            assertionPassed = executeAssertions(testCase.getEffectiveAssertionScript(), lastResponse, runtimeVariables);
            if (!assertionPassed) {
                finalMessage = "Assertion failed";
                finalDetail = "Assertion failed. Script: " + testCase.getEffectiveAssertionScript();
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

        // Optimized pattern to reliably match ${...} even with special characters
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String replacement = null;

            try {
                // Strategy 1: If it's a simple index access like idList[0]
                if (expression.matches("^[a-zA-Z0-9_]+\\[\\d+\\]$")) {
                    int openBracket = expression.indexOf("[");
                    String varName = expression.substring(0, openBracket);
                    int index = Integer.parseInt(expression.substring(openBracket + 1, expression.length() - 1));
                    Object obj = variables.get(varName);

                    if (obj instanceof List) {
                        List<?> list = (List<?>) obj;
                        if (index >= 0 && index < list.size()) {
                            replacement = String.valueOf(list.get(index));
                        }
                    } else if (obj != null && obj.getClass().isArray()) {
                        Object[] arr = (Object[]) obj;
                        if (index >= 0 && index < arr.length) {
                            replacement = String.valueOf(arr[index]);
                        }
                    }
                }

                // Strategy 2: If manual extraction failed, use SpEL
                if (replacement == null
                        && (expression.contains("[") || expression.contains(".") || expression.startsWith("T("))) {
                    StandardEvaluationContext context = new StandardEvaluationContext(variables);
                    context.addPropertyAccessor(new MapAccessor());
                    context.setVariables(variables);
                    Object result = spelParser.parseExpression(expression).getValue(context);
                    if (result != null) {
                        replacement = result.toString();
                    }
                }

                // Strategy 3: Simple variable lookup
                if (replacement == null) {
                    Object value = variables.get(expression);
                    if (value != null) {
                        replacement = value.toString();
                    }
                }
            } catch (Exception e) {
                System.err.println("[DEBUG] Variable replacement failed for: [" + expression + "] - " + e.getMessage());
            }

            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("${" + matcher.group(1) + "}"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private TestResponse executeHttpRequest(String method, String url, String body, String headers) {
        try {
            // Create the base supplier
            java.util.function.Supplier<TestResponse> baseSupplier = () -> {
                WebClient.RequestBodySpec request = webClient
                        .method(org.springframework.http.HttpMethod.valueOf(method))
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
            };

            // Apply Resilience4j decorators: CircuitBreaker first, then Retry
            java.util.function.Supplier<TestResponse> circuitBreakerSupplier = CircuitBreaker
                    .decorateSupplier(circuitBreaker, baseSupplier);
            java.util.function.Supplier<TestResponse> retrySupplier = Retry.decorateSupplier(retry,
                    circuitBreakerSupplier);

            return retrySupplier.get();
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

            // Debug logging
            System.out.println("Extractor execution - Type: " + type + ", Expression: [" + expression + "], Variable: "
                    + extractor.getVariableName());

            // Clean expression: remove any escape characters and fix common issues
            if (expression != null) {
                String originalExpression = expression;

                // 1. Remove all escape characters: \$ -> $
                expression = expression.replace("\\$", "$");

                // 2. Remove ${} wrappers if user accidentally included them: ${token} -> token
                if (expression.startsWith("${") && expression.endsWith("}")) {
                    expression = expression.substring(2, expression.length() - 1);
                    System.out.println("Cleaned expression from ${} wrapper: [" + expression + "]");
                }

                if (expression == null || expression.trim().isEmpty()) {
                    System.err.println("Extractor expression is empty or null!");
                    return null;
                }

                // 3. Fix common issues: $token -> $.token, $data.token -> $.data.token
                if (expression.startsWith("$") && expression.length() > 1 && expression.charAt(1) != '.'
                        && expression.charAt(1) != '[' && expression.charAt(1) != '(') {
                    // If expression is like $token, convert to $.token
                    expression = "$." + expression.substring(1);
                    System.out
                            .println("Fixed expression from $" + originalExpression.substring(1) + " to " + expression);
                }
            }

            switch (type) {
                case "JSONPATH":
                    if (response.getBody() != null) {
                        try {
                            Object result = JsonPath.read(response.getBody(), expression);
                            if (result != null) {
                                return result;
                            }
                        } catch (com.jayway.jsonpath.PathNotFoundException e) {
                            // Path not found is not a critical error, try fallback or return null
                            System.out
                                    .println("JSONPath not found for expression: " + expression + ", trying fallback");
                            // If expression fails and it's a simple path like $.token, try $.data.token
                            if (expression.equals("$.token") || expression.equals("$token")) {
                                try {
                                    Object fallbackResult = JsonPath.read(response.getBody(), "$.data.token");
                                    if (fallbackResult != null) {
                                        System.out.println("Fallback expression $.data.token succeeded");
                                        return fallbackResult;
                                    }
                                } catch (Exception e2) {
                                    System.out.println(
                                            "Fallback expression $.data.token also failed: " + e2.getMessage());
                                }
                            }
                            // Path not found, return null instead of throwing exception
                            System.out.println(
                                    "JSONPath not found, returning null for variable: " + extractor.getVariableName());
                            return null;
                        } catch (Exception e) {
                            // Other exceptions (syntax errors, etc.) should still be logged
                            System.err.println(
                                    "JSONPath execution error for expression [" + expression + "]: " + e.getMessage());
                            // For non-critical errors, return null instead of throwing
                            // This allows the test to continue even if extraction fails
                            return null;
                        }
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
            // Check if it's a PathNotFoundException or similar "not found" error
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("No results for path") ||
                    errorMsg.contains("Path not found") ||
                    e instanceof com.jayway.jsonpath.PathNotFoundException)) {
                // Path not found is expected in some cases, return null instead of throwing
                System.out.println("Extractor path not found for variable: " + extractor.getVariableName()
                        + ", expression: " + extractor.getExpression());
                return null;
            }
            // For other exceptions, log but don't fail the entire test
            System.err.println("Extractor execution failed: " + e.getMessage());
            // Don't throw exception, return null to allow test to continue
            return null;
        }
        return null;
    }

    private boolean executeAssertions(String script, TestResponse response, Map<String, Object> variables) {
        try {
            // Debug logging
            System.out.println("Executing assertion script: " + script);

            // Fix script: replace double quotes with single quotes for JSONPath expressions
            // in jsonPath() calls
            // This prevents Groovy GString interpolation issues with $ characters
            // Pattern: jsonPath(response, "expression") -> jsonPath(response, 'expression')
            script = script.replaceAll("jsonPath\\(response,\\s*\"([^\"]+)\"\\)", "jsonPath(response, '$1')");
            System.out.println("Fixed script (single quotes for JSONPath): " + script);

            // Create a new script engine instance for each execution to avoid state
            // pollution
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");

            // Provide helper functions and objects
            Integer statusCode = response.getStatusCode();
            engine.put("status_code", statusCode);
            engine.put("status", statusCode); // Also provide 'status' for compatibility
            engine.put("response_body", response.getBody());
            engine.put("response", response);
            engine.put("headers", response.getHeaders() != null ? response.getHeaders() : new HashMap<>());

            // Provide vars object for variable manipulation
            Map<String, Object> vars = new HashMap<>(variables);
            engine.put("vars", vars);

            // Provide jsonPath helper function with improved hyphenated key support
            engine.eval("def jsonPath(responseObj, path) {\n" +
                    "  try {\n" +
                    "    def body = (responseObj instanceof com.testing.automation.dto.TestResponse) ? responseObj.getBody() : responseObj.toString();\n"
                    +
                    "    return com.jayway.jsonpath.JsonPath.read(body ?: '{}', path);\n" +
                    "  } catch (Exception e) {\n" +
                    "    System.err.println('jsonPath error for path [' + path + ']: ' + e.getMessage());\n" +
                    "    return null;\n" +
                    "  }\n" +
                    "}");

            // Provide regex helper function
            engine.eval("def regex(text, pattern) {\n" +
                    "  if (text == null) return null;\n" +
                    "  def matcher = (text =~ pattern);\n" +
                    "  if (matcher.find()) {\n" +
                    "    if (matcher.groupCount() > 0) {\n" +
                    "      return matcher.group(1);\n" +
                    "    }\n" +
                    "  }\n" +
                    "  return null;\n" +
                    "}");

            // Add all runtime variables to script context
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }

            Object result = engine.eval(script);

            // Update variables from vars object if modified
            // This is critical: vars.put() modifies the vars map, which we need to sync
            // back
            variables.putAll(vars);
            System.out.println("Variables after assertion script: " + variables);

            // If script doesn't return a boolean, consider it successful if no exception
            // was thrown
            // This handles cases like vars.put() which returns the value, not a boolean
            if (result == null) {
                // Script executed without error, consider it successful
                System.out.println("Assertion script returned null, treating as success");
                return true;
            }

            // If result is a Boolean, return it
            if (result instanceof Boolean) {
                System.out.println("Assertion script returned boolean: " + result);
                return (Boolean) result;
            }

            // If result is not null and not false, consider it successful
            System.out.println("Assertion script returned non-boolean: " + result + ", treating as success");
            return result != null;
        } catch (Throwable e) {
            System.err.println("Assertion execution failed: " + e.getMessage());
            // e.printStackTrace(); // Optional: reduce log noise for expected assertion
            // failures
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