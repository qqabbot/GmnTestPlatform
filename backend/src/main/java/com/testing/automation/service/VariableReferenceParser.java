package com.testing.automation.service;

import com.testing.automation.dto.RunnerContext;
import com.testing.automation.dto.TestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for variable reference syntax
 * Supports:
 * - ${variable_name} - Standard variable reference
 * - {{StepName.field.path}} - Step response reference
 */
@Slf4j
@Component
public class VariableReferenceParser {
    
    // Pattern for ${variable} syntax
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    // Pattern for {{StepName.field.path}} syntax
    private static final Pattern STEP_REFERENCE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Resolve all variable references in a string
     * @param expression expression containing variable references
     * @param context runner context
     * @return resolved string
     */
    public String resolveVariables(String expression, RunnerContext context) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }
        
        String result = expression;
        
        // Resolve step references first: {{StepName.field.path}}
        result = resolveStepReferences(result, context);
        
        // Then resolve standard variables: ${variable}
        result = resolveStandardVariables(result, context);
        
        return result;
    }
    
    /**
     * Resolve step reference syntax: {{StepName.field.path}}
     */
    private String resolveStepReferences(String expression, RunnerContext context) {
        Matcher matcher = STEP_REFERENCE_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String reference = matcher.group(1); // e.g., "LoginStep.response.body.data.token"
            String resolved = resolveStepReference(reference, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Resolve a single step reference
     * Format: StepName.field.path
     * Examples:
     * - LoginStep.response.body.data.token
     * - Step1.response.headers.Authorization
     * - Step2.response.code
     */
    private String resolveStepReference(String reference, RunnerContext context) {
        try {
            String[] parts = reference.split("\\.", 2);
            if (parts.length < 2) {
                log.warn("Invalid step reference format: {}", reference);
                return "{{" + reference + "}}"; // Return original if invalid
            }
            
            String stepName = parts[0].trim();
            String path = parts[1].trim();
            
            // Get step ID by name
            Long stepId = context.getStepIdByName(stepName);
            if (stepId == null) {
                log.warn("Step not found: {}", stepName);
                return "{{" + reference + "}}";
            }
            
            // Get step execution result
            TestResult stepResult = context.getStepResult(stepId);
            if (stepResult == null) {
                log.warn("Step result not found for step: {}", stepName);
                return "{{" + reference + "}}";
            }
            
            // Resolve path
            return resolveStepPath(stepResult, path);
            
        } catch (Exception e) {
            log.error("Error resolving step reference {}: {}", reference, e.getMessage(), e);
            return "{{" + reference + "}}";
        }
    }
    
    /**
     * Resolve path in step result
     * Supports: response.body.data.token, response.headers.Authorization, response.code
     */
    private String resolveStepPath(TestResult stepResult, String path) {
        try {
            String[] parts = path.split("\\.");
            if (parts.length < 2) {
                return path; // Invalid path
            }
            
            String source = parts[0]; // response
            String field = parts[1];   // body, headers, code
            
            if ("response".equalsIgnoreCase(source)) {
                if ("code".equalsIgnoreCase(field)) {
                    return String.valueOf(stepResult.getResponseCode());
                } else if ("headers".equalsIgnoreCase(field)) {
                    if (parts.length >= 3 && stepResult.getResponseHeaders() != null) {
                        String headerName = parts[2];
                        Object headerValue = stepResult.getResponseHeaders().get(headerName);
                        return headerValue != null ? headerValue.toString() : "";
                    }
                } else if ("body".equalsIgnoreCase(field)) {
                    if (parts.length >= 3 && stepResult.getResponseBody() != null) {
                        // Use JSONPath to extract value
                        String jsonPath = "$." + String.join(".", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                        try {
                            Object value = com.jayway.jsonpath.JsonPath.read(stepResult.getResponseBody(), jsonPath);
                            return value != null ? value.toString() : "";
                        } catch (Exception e) {
                            log.warn("Failed to extract JSONPath {} from response: {}", jsonPath, e.getMessage());
                            return "";
                        }
                    }
                }
            }
            
            return path; // Return original if cannot resolve
        } catch (Exception e) {
            log.error("Error resolving step path {}: {}", path, e.getMessage(), e);
            return path;
        }
    }
    
    /**
     * Resolve standard variable syntax: ${variable}
     */
    private String resolveStandardVariables(String expression, RunnerContext context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.getVariable(varName);
            String replacement = value != null ? value.toString() : "${" + varName + "}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
