package com.testing.automation.service;

import com.testing.automation.dto.TestResult;
import com.testing.automation.mapper.TestCaseMapper;
import com.testing.automation.mapper.TestPlanMapper;
import com.testing.automation.model.TestCase;
import com.testing.automation.model.TestPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            // We just need to enrich them with steps.
            for (int i = 0; i < plan.getTestCases().size(); i++) {
                TestCase tc = plan.getTestCases().get(i);
                TestCase details = caseMapper.findByIdWithDetails(tc.getId());
                if (details != null) {
                    details.setParameterOverrides(tc.getParameterOverrides());
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
}
