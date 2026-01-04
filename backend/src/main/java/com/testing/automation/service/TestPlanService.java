package com.testing.automation.service;

import com.testing.automation.dto.TestResult;
import com.testing.automation.Mapper.TestCaseMapper;
import com.testing.automation.Mapper.TestPlanMapper;
import com.testing.automation.model.TestCase;
import com.testing.automation.model.TestPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.testing.automation.dto.VariableContext;
import com.testing.automation.model.TestStep;
import com.testing.automation.dto.TestPlanCaseOverride;

@Service
public class TestPlanService {

    @Autowired
    private TestPlanMapper planMapper;
    @Autowired
    private TestCaseMapper caseMapper;
    @Autowired
    private TestCaseService testCaseService;
    @Autowired
    private GlobalVariableService globalVariableService;

    public List<TestPlan> findAll() {
        return planMapper.findAll();
    }

    public List<TestPlan> findByProjectId(Long projectId) {
        return planMapper.findByProjectId(projectId);
    }

    public TestPlan findById(Long id) {
        TestPlan plan = planMapper.findById(id);
        if (plan == null) {
            throw new RuntimeException("Test Plan not found");
        }

        // At this point, plan.getTestCases() should already be populated by MyBatis
        // @Many.
        // However, we need 'details' (steps, etc.) for each case which caseMapper
        // provides.
        // Let's merge the details into the already fetched cases to keep
        // parameterOverrides.
        if (plan.getTestCases() != null) {
            for (TestCase tc : plan.getTestCases()) {
                TestCase details = caseMapper.findByIdWithDetails(tc.getId());
                if (details != null) {
                    // Update details with overrides from the join table fetch
                    // Copy basic fields but keep the overrides
                    details.setParameterOverrides(tc.getParameterOverrides());
                    // Replace in list or update in place?
                    // Let's update the plan list directly if it's mutable.
                }
            }
            // Actually, findCasesByPlanId already returned TestCase objects with overrides.
            // We just need to enrich them with steps and preserve all overrides.
            for (int i = 0; i < plan.getTestCases().size(); i++) {
                TestCase tc = plan.getTestCases().get(i);
                TestCase details = caseMapper.findByIdWithDetails(tc.getId());
                if (details != null) {
                    // Preserve ALL overrides from the plan-case relationship
                    details.setParameterOverrides(tc.getParameterOverrides());
                    details.setCaseNameOverride(tc.getCaseNameOverride());
                    details.setUrlOverride(tc.getUrlOverride());
                    details.setMethodOverride(tc.getMethodOverride());
                    details.setHeadersOverride(tc.getHeadersOverride());
                    details.setBodyOverride(tc.getBodyOverride());
                    details.setAssertionScriptOverride(tc.getAssertionScriptOverride());
                    details.setStepsOverride(tc.getStepsOverride());
                    details.setPlanEnabled(tc.getPlanEnabled());
                    plan.getTestCases().set(i, details);
                }
            }
        }
        return plan;
    }

    @Transactional
    public TestPlan create(TestPlan plan) {
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.insert(plan);

        // Handle ManyToMany: Insert test cases into join table
        if (plan.getTestCases() != null) {
            for (int i = 0; i < plan.getTestCases().size(); i++) {
                TestCase testCase = plan.getTestCases().get(i);
                planMapper.addCaseToPlan(plan.getId(), testCase.getId(), i, testCase.getParameterOverrides());
            }
        }
        return plan;
    }

    @Transactional
    public TestPlan update(Long id, TestPlan updatedPlan) {
        TestPlan existing = planMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Test Plan not found");
        }

        existing.setName(updatedPlan.getName());
        existing.setDescription(updatedPlan.getDescription());
        existing.setProjectId(updatedPlan.getProjectId());
        existing.setUpdatedAt(LocalDateTime.now());
        planMapper.update(existing);

        // Update ManyToMany relationship
        planMapper.removeCasesFromPlan(id); // Remove all existing
        if (updatedPlan.getTestCases() != null) {
            for (int i = 0; i < updatedPlan.getTestCases().size(); i++) {
                TestCase testCase = updatedPlan.getTestCases().get(i);
                planMapper.addCaseToPlan(id, testCase.getId(), i, testCase.getParameterOverrides());
            }
        }

        return existing;
    }

    @Transactional
    public void delete(Long id) {
        planMapper.removeCasesFromPlan(id); // Remove join table entries first
        planMapper.deleteById(id);
    }

    /**
     * Save plan-specific overrides for a test case
     * This does NOT modify the original test_case table
     */
    @Transactional
    public void saveCaseOverrides(Long planId, Long caseId, TestPlanCaseOverride overrides) {
        planMapper.updateCaseOverrides(planId, caseId, overrides);
    }

    /**
     * Execute a Test Plan
     */
    public List<TestResult> executePlan(Long planId, String envKey) {
        TestPlan plan = findById(planId);
        List<TestResult> results = new ArrayList<>();

        Long projectId = plan.getProjectId();
        // Base runtime variables from Environment
        Map<String, Object> baseRuntimeVariables = globalVariableService.getVariablesMapWithInheritance(projectId, null,
                envKey);
        Map<Long, TestResult> executionHistory = new HashMap<>();

        for (TestCase testCase : plan.getTestCases()) {
            try {
                // Create a COPY of runtime variables for this step to avoid polluting the
                // global scope permanently?
                // Step 603 analysis said: "Sequential execution context: Create a SHARED
                // runtimeVariables Map."
                // So we should use the same map reference.

                // However, we need to inject Overrides.
                // If overrides are specific to this step, they should effectively "put" into
                // the map.
                // But do they persist for subsequent steps?
                // Usually "Input Parameters" for a step (like `id=123`) might be specific.
                // But if they are "Global Variables" (like `token=abc`), they should persist.
                // Let's assume they act like "Setting a variable before execution" = they
                // persist.

                if (testCase.getParameterOverrides() != null && !testCase.getParameterOverrides().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> overrides = mapper.readValue(testCase.getParameterOverrides(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                });
                        baseRuntimeVariables.putAll(overrides);
                    } catch (Exception e) {
                        System.err.println("Failed to parse parameter overrides for case " + testCase.getId() + ": "
                                + e.getMessage());
                    }
                }

                TestResult result = testCaseService.executeSingleCaseLogic(testCase, baseRuntimeVariables,
                        executionHistory);
                results.add(result);
                executionHistory.put(testCase.getId(), result);
            } catch (Exception e) {
                results.add(TestResult.builder()
                        .caseId(testCase.getId())
                        .caseName(testCase.getCaseName())
                        .status("FAIL")
                        .message("Execution Error: " + e.getMessage())
                        .build());
            }
        }
        return results;
    }

    /**
     * Analyze a test plan and extract all variables produced/consumed at each step
     */
    public Map<Integer, VariableContext> analyzePlanVariables(Long planId) {
        TestPlan plan = findById(planId);
        Map<Integer, VariableContext> analysis = new HashMap<>();
        Set<String> cumulativeVariables = new HashSet<>();

        // Add built-in variables
        cumulativeVariables.add("base_url");
        cumulativeVariables.add("timestamp");

        List<TestCase> cases = plan.getTestCases();
        if (cases == null)
            return analysis;

        for (int i = 0; i < cases.size(); i++) {
            TestCase testCase = cases.get(i);
            VariableContext context = new VariableContext();
            Set<String> produced = extractProducedVariables(testCase);
            Set<String> consumed = extractConsumedVariables(testCase);

            context.setProducedVariables(new ArrayList<>(produced));
            context.setConsumedVariables(new ArrayList<>(consumed));
            context.setAvailableVariables(new ArrayList<>(cumulativeVariables));

            // Add produced variables to cumulative set for next cases
            cumulativeVariables.addAll(produced);

            analysis.put(i, context);
        }

        return analysis;
    }

    /**
     * Extract variables that are produced (set) by a test case
     */
    private Set<String> extractProducedVariables(TestCase testCase) {
        Set<String> variables = new HashSet<>();
        Pattern varsPutPattern = Pattern.compile("vars\\.put\\([\"']([^\"']+)[\"']");

        // Check global assertion script
        if (testCase.getEffectiveAssertionScript() != null) {
            Matcher matcher = varsPutPattern.matcher(testCase.getEffectiveAssertionScript());
            while (matcher.find()) {
                variables.add(matcher.group(1));
            }
        }

        // Check each step's assertion script
        List<TestStep> effectiveSteps = testCase.getEffectiveSteps();
        if (effectiveSteps != null) {
            for (TestStep step : effectiveSteps) {
                if (step.getAssertionScript() != null) {
                    Matcher matcher = varsPutPattern.matcher(step.getAssertionScript());
                    while (matcher.find()) {
                        variables.add(matcher.group(1));
                    }
                }
            }
        }

        return variables;
    }

    /**
     * Extract variables that are consumed (used) by a test case
     */
    private Set<String> extractConsumedVariables(TestCase testCase) {
        Set<String> variables = new HashSet<>();
        Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)\\}");

        // Check URL
        if (testCase.getEffectiveUrl() != null) {
            Matcher matcher = variablePattern.matcher(testCase.getEffectiveUrl());
            while (matcher.find()) {
                variables.add(matcher.group(1));
            }
        }

        // Check body
        if (testCase.getEffectiveBody() != null) {
            Matcher matcher = variablePattern.matcher(testCase.getEffectiveBody());
            while (matcher.find()) {
                variables.add(matcher.group(1));
            }
        }

        // Check steps
        List<TestStep> effectiveSteps = testCase.getEffectiveSteps();
        if (effectiveSteps != null) {
            for (TestStep step : effectiveSteps) {
                if (step.getUrl() != null) {
                    Matcher matcher = variablePattern.matcher(step.getUrl());
                    while (matcher.find()) {
                        variables.add(matcher.group(1));
                    }
                }
                if (step.getBody() != null) {
                    Matcher matcher = variablePattern.matcher(step.getBody());
                    while (matcher.find()) {
                        variables.add(matcher.group(1));
                    }
                }
            }
        }

        return variables;
    }

    /**
     * Suggest common parameters for a test case based on its content
     */
    public List<String> suggestParameters(Long caseId) {
        TestCase testCase = caseMapper.findByIdWithDetails(caseId);
        if (testCase == null) {
            return new ArrayList<>();
        }

        Set<String> parameters = extractConsumedVariables(testCase);
        return new ArrayList<>(parameters);
    }
}
