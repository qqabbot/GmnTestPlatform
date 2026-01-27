package com.testing.automation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.model.TestCase;
import com.testing.automation.Mapper.TestCaseMapper;
import com.testing.automation.Mapper.ScenarioExecutionRecordMapper;
import com.testing.automation.Mapper.ScenarioStepExecutionLogMapper;
import com.testing.automation.model.ScenarioExecutionRecord;
import com.testing.automation.model.ScenarioStepExecutionLog;
import com.testing.automation.model.TestScenario;
import com.testing.automation.service.invoker.StepInvoker;
import com.testing.automation.service.invoker.StepInvokerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
public class ScenarioExecutionEngine {

    @Autowired
    private TestScenarioService scenarioService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private GlobalVariableService globalVariableService;

    @Autowired
    private ScenarioExecutionRecordMapper executionRecordMapper;

    @Autowired
    private ScenarioStepExecutionLogMapper stepLogMapper;

    @Autowired
    private StepInvokerRegistry invokerRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TestResult> executeScenario(Long scenarioId, String envKey) {
        return executeScenario(scenarioId, envKey, null);
    }

    public List<TestResult> executeScenario(Long scenarioId, String envKey,
            Consumer<ScenarioExecutionEvent> eventListener) {
        return executeScenario(scenarioId, envKey, eventListener, null);
    }

    /**
     * Execute scenario with optional parent context (for nested scenarios)
     */
    public List<TestResult> executeScenario(Long scenarioId, String envKey,
            Consumer<ScenarioExecutionEvent> eventListener, RunnerContext parentContext) {
        // Create or use parent context
        RunnerContext runnerContext = parentContext != null ? parentContext : new RunnerContext();

        // Check for circular reference
        if (runnerContext.isExecutingScenario(scenarioId)) {
            throw new RuntimeException(
                    "Circular reference detected: scenario " + scenarioId + " is already being executed");
        }

        TestScenario scenario = scenarioService.findById(scenarioId);
        if (scenario == null) {
            throw new RuntimeException("Scenario not found: " + scenarioId);
        }

        // Add to executing set
        runnerContext.addExecutingScenario(scenarioId);

        try {
            // Build Execution Context with scope stack (only if new context)
            if (parentContext == null) {
                Map<String, Object> globalVars = globalVariableService.getVariablesMapWithInheritance(
                        scenario.getProjectId(), null, envKey);
                // Initialize global scope with environment variables
                for (Map.Entry<String, Object> entry : globalVars.entrySet()) {
                    runnerContext.setGlobalVariable(entry.getKey(), entry.getValue());
                }
            }
            // Store envKey in context for nested scenarios
            runnerContext.setVariable("_envKey", envKey);

            // Notify scenario start
            if (eventListener != null) {
                eventListener.accept(ScenarioExecutionEvent.builder()
                        .type("scenario_start")
                        .stepName(scenario.getName())
                        .variables(new HashMap<>(runnerContext.getAllVariables()))
                        .timestamp(System.currentTimeMillis())
                        .build());
            }

            // Create execution record (only for top-level scenarios, not nested)
            ScenarioExecutionRecord record = null;
            Long recordId = null;
            if (parentContext == null) { // Only create record for top-level scenario
                record = new ScenarioExecutionRecord();
                record.setScenarioId(scenarioId);
                record.setScenarioName(scenario.getName());
                record.setEnvKey(envKey);
                record.setStatus("RUNNING");
                record.setStartedAt(LocalDateTime.now());
                executionRecordMapper.insert(record);
                recordId = record.getId();
            }

            try {
                // Get Step Tree
                List<TestScenarioStepDTO> stepTree = scenarioService.getScenarioStepsTree(scenarioId);

                // Execute recursively using Step Invokers
                List<TestResult> results = new ArrayList<>();
                // Execution history is now managed by RunnerContext

                long startTime = System.currentTimeMillis();
                for (TestScenarioStepDTO step : stepTree) {
                    executeStepRecursively(step, runnerContext, results, recordId, eventListener);
                }
                long duration = System.currentTimeMillis() - startTime;

                // Update execution record with final status (only for top-level)
                if (record != null) {
                    int passed = (int) results.stream().filter(r -> "PASS".equals(r.getStatus())).count();
                    int failed = (int) results.stream().filter(r -> "FAIL".equals(r.getStatus())).count();

                    record.setTotalSteps(results.size());
                    record.setPassedSteps(passed);
                    record.setFailedSteps(failed);
                    record.setDurationMs(duration);
                    record.setCompletedAt(LocalDateTime.now());
                    record.setStatus(failed > 0 ? "FAIL" : "PASS");
                    executionRecordMapper.update(record);

                    // Notify scenario complete
                    if (eventListener != null) {
                        eventListener.accept(ScenarioExecutionEvent.builder()
                                .type("scenario_complete")
                                .status(record.getStatus())
                                .timestamp(System.currentTimeMillis())
                                .build());
                    }
                }

                return results;
            } catch (Exception e) {
                // Mark as failed on exception (only for top-level)
                if (record != null) {
                    record.setStatus("FAIL");
                    record.setCompletedAt(LocalDateTime.now());
                    executionRecordMapper.update(record);
                }
                throw e;
            } finally {
                // Remove from executing set
                runnerContext.removeExecutingScenario(scenarioId);
            }
        } finally {
            runnerContext.removeExecutingScenario(scenarioId);
        }
    }

    private void executeStepRecursively(TestScenarioStepDTO step,
            RunnerContext context,
            List<TestResult> results,
            Long recordId,
            Consumer<ScenarioExecutionEvent> eventListener) {

        // Check if step is disabled
        if (isStepDisabled(step)) {
            log.debug("Step {} is disabled, skipping", step.getId());
            return;
        }

        String type = step.getType();

        // Notify step start
        if (eventListener != null) {
            eventListener.accept(ScenarioExecutionEvent.builder()
                    .type("step_start")
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .status("RUNNING")
                    .timestamp(System.currentTimeMillis())
                    .build());
        }

        // Use Step Invoker Pattern
        StepInvoker invoker = invokerRegistry.getInvoker(type);
        if (invoker != null) {
            try {
                invoker.execute(step, context, results, eventListener);

                // Save step log for CASE steps (other steps may not have TestResult)
                TestResult stepResult = context.getStepResult(step.getId());
                if (stepResult != null && recordId != null) {
                    saveStepLog(recordId, step, stepResult);
                }
            } catch (Exception e) {
                log.error("Error executing step {}: {}", step.getId(), e.getMessage(), e);
                TestResult errorResult = TestResult.builder()
                        .caseId(step.getId())
                        .caseName(step.getName())
                        .status("FAIL")
                        .message("Execution Error: " + e.getMessage())
                        .build();
                results.add(errorResult);
                context.recordStepResult(step.getId(), errorResult);
            }
        } else {
            log.warn("No invoker found for step type: {}", type);
        }
    }

    /**
     * Check if step is disabled
     * Steps can be disabled via dataOverrides.enabled flag
     */
    private boolean isStepDisabled(TestScenarioStepDTO step) {
        if (step.getDataOverrides() == null) {
            return false;
        }
        try {
            JsonNode overrides = objectMapper.valueToTree(step.getDataOverrides());
            if (overrides.has("enabled")) {
                return !overrides.get("enabled").asBoolean();
            }
        } catch (Exception e) {
            log.warn("Failed to parse dataOverrides for step {}: {}", step.getId(), e.getMessage());
        }
        return false;
    }

    private void saveStepLog(Long recordId, TestScenarioStepDTO step, TestResult result) {
        try {
            ScenarioStepExecutionLog log = new ScenarioStepExecutionLog();
            log.setRecordId(recordId);
            log.setStepId(step.getId());
            log.setStepName(step.getName());
            log.setStepType(step.getType());
            log.setStatus(result.getStatus());
            log.setRequestUrl(result.getRequestUrl());
            log.setRequestMethod(result.getMethod());
            log.setRequestBody(result.getRequestBody());
            log.setRequestHeaders(result.getResponseHeaders() != null ? objectMapper.writeValueAsString(result.getResponseHeaders()) : null); // Fallback if needed, but better use a dedicated field if added
            log.setResponseCode(result.getResponseCode());
            log.setResponseBody(result.getResponseBody());
            log.setResponseHeaders(result.getResponseHeaders() != null ? objectMapper.writeValueAsString(result.getResponseHeaders()) : null);
            log.setDurationMs(result.getDuration() != null ? result.getDuration().longValue() : null);
            log.setErrorMessage(result.getDetail());
            log.setExecutedAt(LocalDateTime.now());

            stepLogMapper.insert(log);
        } catch (Exception e) {
            log.error("Failed to save step log: {}", e.getMessage(), e);
        }
    }

    private void evaluateVisualAssertions(TestResult result, JsonNode assertions, Map<String, Object> context) {
        if (assertions == null || !assertions.isArray())
            return;

        StringBuilder failures = new StringBuilder();
        for (JsonNode rule : assertions) {
            try {
                String source = rule.has("source") ? rule.get("source").asText() : "JSON_BODY";
                String property = rule.has("property") ? rule.get("property").asText() : "";
                String operator = rule.has("operator") ? rule.get("operator").asText() : "EQUALS";
                String expectedStr = rule.has("expected") ? rule.get("expected").asText() : "";

                // Resolve variables in expected value
                String resolvedExpected = testCaseService.replaceVariables(expectedStr, context);

                Object actualValue = null;
                if ("STATUS_CODE".equalsIgnoreCase(source)) {
                    actualValue = String.valueOf(result.getResponseCode());
                } else if ("RESPONSE_HEADER".equalsIgnoreCase(source)) {
                    if (result.getResponseHeaders() != null) {
                        actualValue = result.getResponseHeaders().get(property);
                    }
                } else if ("JSON_BODY".equalsIgnoreCase(source)) {
                    if (result.getResponseBody() != null) {
                        try {
                            actualValue = com.jayway.jsonpath.JsonPath.read(result.getResponseBody(), property);
                        } catch (Exception e) {
                            actualValue = "PROPERTY_NOT_FOUND";
                        }
                    }
                }

                boolean passed = compareValues(actualValue, resolvedExpected, operator);
                if (!passed) {
                    failures.append(String.format("- [%s] %s %s %s (Actual: %s)\n",
                            source, property, operator, resolvedExpected, actualValue));
                }
            } catch (Exception e) {
                failures.append("- Error evaluating rule: ").append(e.getMessage()).append("\n");
            }
        }

        if (failures.length() > 0) {
            result.setStatus("FAIL");
            String currentDetail = result.getDetail();
            result.setDetail((currentDetail != null ? currentDetail + "\n" : "")
                    + "Visual Assertion Failures:\n" + failures.toString());
        }
    }

    private boolean compareValues(Object actual, String expected, String operator) {
        String actualStr = actual == null ? "null" : String.valueOf(actual);
        switch (operator.toUpperCase()) {
            case "EQUALS":
                return actualStr.equals(expected);
            case "NOT_EQUALS":
                return !actualStr.equals(expected);
            case "CONTAINS":
                return actualStr.contains(expected);
            case "NOT_CONTAINS":
                return !actualStr.contains(expected);
            case "EXISTS":
                return actual != null && !"PROPERTY_NOT_FOUND".equals(actual);
            case "NOT_EXISTS":
                return actual == null || "PROPERTY_NOT_FOUND".equals(actual);
            case "GREATER_THAN":
                try {
                    return Double.parseDouble(actualStr) > Double.parseDouble(expected);
                } catch (Exception e) {
                    return false;
                }
            case "LESS_THAN":
                try {
                    return Double.parseDouble(actualStr) < Double.parseDouble(expected);
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }
}
