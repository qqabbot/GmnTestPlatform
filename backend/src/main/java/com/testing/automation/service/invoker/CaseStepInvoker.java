package com.testing.automation.service.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestResult;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.model.TestCase;
import com.testing.automation.Mapper.TestCaseMapper;
import com.testing.automation.service.TestCaseService;
import com.testing.automation.service.VariableReferenceParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class CaseStepInvoker implements StepInvoker {

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private VariableReferenceParser variableParser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSupportedType() {
        return "CASE";
    }

    @Override
    public void execute(TestScenarioStepDTO step,
            RunnerContext context,
            List<TestResult> results,
            Consumer<ScenarioExecutionEvent> eventListener) {
        if (step.getReferenceCaseId() == null) {
            log.warn("Case step {} has no reference case ID, skipping", step.getId());
            return;
        }

        TestCase testCase = testCaseMapper.findByIdWithDetails(step.getReferenceCaseId());
        if (testCase == null) {
            log.error("Case not found: {}", step.getReferenceCaseId());
            return;
        }

        try {
            String stepName = step.getName() != null ? step.getName() : testCase.getCaseName();
            log.info(">>> Start Executing Case Step: {}", stepName);

            // Register step for step reference syntax
            context.registerStep(step.getId(), stepName);

            // Apply Data Overrides from step configuration
            Map<String, Object> stepContext = new HashMap<>(context.getAllVariables());
            TestCase effectiveTestCase = applyDataOverrides(testCase, step, stepContext);

            // Update context with step-specific variables
            for (Map.Entry<String, Object> entry : stepContext.entrySet()) {
                if (!context.hasVariable(entry.getKey())) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            TestResult result = testCaseService.executeSingleCaseLogic(
                    effectiveTestCase,
                    stepContext,
                    context.getExecutionHistory());
            result.setCaseName(stepName);

            // Evaluate Visual Assertions
            if (step.getDataOverrides() != null) {
                try {
                    JsonNode overrides = objectMapper.valueToTree(step.getDataOverrides());
                    if (overrides.has("visualAssertions")) {
                        evaluateVisualAssertions(result, overrides.get("visualAssertions"), context);
                    }
                } catch (Exception e) {
                    log.warn("Failed to evaluate visual assertions: {}", e.getMessage());
                }
            }

            log.info("<<< Finished Case Step. Status: {}", result.getStatus());
            if ("FAIL".equals(result.getStatus())) {
                log.error("    Error: {}", result.getDetail());
            }

            results.add(result);
            context.recordStepResult(step.getId(), result);

            // Notify step complete
            if (eventListener != null) {
                eventListener.accept(ScenarioExecutionEvent.builder()
                        .type("step_complete")
                        .stepId(step.getId())
                        .stepName(stepName)
                        .status(result.getStatus())
                        .variables(new HashMap<>(context.getAllVariables()))
                        .timestamp(System.currentTimeMillis())
                        .build());
            }

            // Extract variables from response
            extractVariables(step, result, context);

            // Propagate changed variables back to the global context
            // This ensures variables put in vars (e.g., via script) are available for next
            // steps
            for (Map.Entry<String, Object> entry : stepContext.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            log.error("Exception executing case step: {}", e.getMessage(), e);
            TestResult errorResult = TestResult.builder()
                    .caseId(step.getId())
                    .caseName(step.getName())
                    .status("FAIL")
                    .message("Execution Error: " + e.getMessage())
                    .build();
            results.add(errorResult);
        }
    }

    private TestCase applyDataOverrides(TestCase originalCase, TestScenarioStepDTO step, Map<String, Object> context) {
        if (step.getDataOverrides() == null) {
            return originalCase;
        }

        try {
            JsonNode overrides = objectMapper.valueToTree(step.getDataOverrides());

            // Create a copy of the test case
            TestCase effectiveCase = new TestCase();
            effectiveCase.setId(originalCase.getId());
            effectiveCase.setModuleId(originalCase.getModuleId());
            effectiveCase.setCaseName(originalCase.getCaseName());
            effectiveCase.setMethod(originalCase.getMethod());
            effectiveCase.setUrl(originalCase.getUrl());
            effectiveCase.setHeaders(originalCase.getHeaders());
            effectiveCase.setBody(originalCase.getBody());
            effectiveCase.setPrecondition(originalCase.getPrecondition());
            effectiveCase.setSetupScript(originalCase.getSetupScript());
            effectiveCase.setAssertionScript(originalCase.getAssertionScript());
            effectiveCase.setIsActive(originalCase.getIsActive());
            effectiveCase.setSteps(originalCase.getSteps());

            log.info("[Data Overrides] Original Case Body: {}", originalCase.getBody());
            log.info("[Data Overrides] Full Overrides JSON: {}", overrides.toString());

            // Apply overrides
            if (overrides.has("url") && !overrides.get("url").isNull()) {
                effectiveCase.setUrl(overrides.get("url").asText());
            }
            if (overrides.has("method") && !overrides.get("method").isNull()) {
                effectiveCase.setMethod(overrides.get("method").asText());
            }
            if (overrides.has("headers") && !overrides.get("headers").isNull()) {
                if (overrides.get("headers").isObject()) {
                    Map<String, String> mergedHeaders = new HashMap<>();
                    if (originalCase.getHeaders() != null && !originalCase.getHeaders().trim().isEmpty()) {
                        try {
                            Map<String, String> originalHeaders = objectMapper.readValue(
                                    originalCase.getHeaders(),
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                                    });
                            mergedHeaders.putAll(originalHeaders);
                        } catch (Exception e) {
                            log.warn("Failed to parse original headers: {}", e.getMessage());
                        }
                    }
                    overrides.get("headers").fields().forEachRemaining(entry -> {
                        mergedHeaders.put(entry.getKey(), entry.getValue().asText());
                    });
                    effectiveCase.setHeaders(objectMapper.writeValueAsString(mergedHeaders));
                } else if (overrides.get("headers").isTextual()) {
                    effectiveCase.setHeaders(overrides.get("headers").asText());
                }
            }
            if (overrides.has("body") && !overrides.get("body").isNull()) {
                JsonNode bodyNode = overrides.get("body");
                effectiveCase.setBody(bodyNode.isTextual() ? bodyNode.asText() : bodyNode.toString());
            }
            if (overrides.has("params") && overrides.get("params").isObject()) {
                JsonNode params = overrides.get("params");
                params.fields().forEachRemaining(entry -> {
                    String value = entry.getValue().isTextual()
                            ? entry.getValue().asText()
                            : entry.getValue().toString();
                    context.put(entry.getKey(), value);
                });
            }

            // Apply setupScript (Pre-request Script) override if present
            if (overrides.has("setupScript") && !overrides.get("setupScript").isNull()) {
                effectiveCase.setSetupScript(overrides.get("setupScript").asText());
            }

            // Apply assertionScript (Global Assertion) override if present
            if (overrides.has("assertionScript") && !overrides.get("assertionScript").isNull()) {
                effectiveCase.setAssertionScript(overrides.get("assertionScript").asText());
            }

            return effectiveCase;
        } catch (Exception e) {
            log.error("Failed to apply data overrides, using original case: {}", e.getMessage(), e);
            return originalCase;
        }
    }

    private void extractVariables(TestScenarioStepDTO step, TestResult result, RunnerContext context) {
        if (step.getDataOverrides() == null) {
            return;
        }

        try {
            JsonNode overrides = objectMapper.valueToTree(step.getDataOverrides());
            if (overrides.has("extract") && overrides.get("extract").isArray()) {
                for (JsonNode extractRule : overrides.get("extract")) {
                    String varName = extractRule.get("varName").asText();
                    String type = extractRule.has("type") ? extractRule.get("type").asText() : "JSON";
                    String expression = extractRule.has("expression") ? extractRule.get("expression").asText()
                            : (extractRule.has("jsonPath") ? extractRule.get("jsonPath").asText() : "");

                    try {
                        Object extractedValue = null;
                        if ("JSON".equalsIgnoreCase(type)) {
                            if (result.getResponseBody() != null) {
                                extractedValue = com.jayway.jsonpath.JsonPath.read(result.getResponseBody(),
                                        expression);
                            }
                        } else if ("REGEX".equalsIgnoreCase(type)) {
                            if (result.getResponseBody() != null) {
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(expression);
                                java.util.regex.Matcher matcher = pattern.matcher(result.getResponseBody());
                                if (matcher.find()) {
                                    int group = extractRule.has("group") ? extractRule.get("group").asInt() : 0;
                                    extractedValue = matcher.group(group);
                                }
                            }
                        } else if ("HEADER".equalsIgnoreCase(type)) {
                            if (result.getResponseHeaders() != null) {
                                extractedValue = result.getResponseHeaders().get(expression);
                            }
                        }

                        if (extractedValue != null) {
                            context.setVariable(varName, extractedValue);
                            log.info("  Extracted variable [{}]: {} = {}", type, varName, extractedValue);
                        }
                    } catch (Exception e) {
                        log.warn("  Failed to extract {} ({}): {}", varName, type, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process variable extraction: {}", e.getMessage());
        }
    }

    private void evaluateVisualAssertions(TestResult result, JsonNode assertions, RunnerContext context) {
        if (assertions == null || !assertions.isArray()) {
            return;
        }

        StringBuilder failures = new StringBuilder();
        for (JsonNode rule : assertions) {
            try {
                String source = rule.has("source") ? rule.get("source").asText() : "JSON_BODY";
                String property = rule.has("property") ? rule.get("property").asText() : "";
                String operator = rule.has("operator") ? rule.get("operator").asText() : "EQUALS";
                String expectedStr = rule.has("expected") ? rule.get("expected").asText() : "";

                // Resolve variables in expected value (supports both ${var} and {{Step.field}}
                // syntax)
                String resolvedExpected = variableParser.resolveVariables(expectedStr, context);

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
