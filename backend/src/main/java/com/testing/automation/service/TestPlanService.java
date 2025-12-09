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
        // Load test cases from ManyToMany relationship
        List<Long> caseIds = planMapper.findCaseIdsByPlanId(id);
        List<TestCase> testCases = new ArrayList<>();
        for (Long caseId : caseIds) {
            TestCase testCase = caseMapper.findById(caseId);
            if (testCase != null) {
                testCases.add(testCase);
            }
        }
        plan.setTestCases(testCases);
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
                planMapper.addCaseToPlan(plan.getId(), testCase.getId(), i);
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
                planMapper.addCaseToPlan(id, testCase.getId(), i);
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
        Map<String, Object> runtimeVariables = globalVariableService.getVariablesMapWithInheritance(projectId, null,
                envKey);
        Map<Long, TestResult> executionHistory = new HashMap<>();

        for (TestCase testCase : plan.getTestCases()) {
            try {
                TestResult result = testCaseService.executeSingleCaseLogic(testCase, runtimeVariables,
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
