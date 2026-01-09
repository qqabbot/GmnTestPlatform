package com.testing.automation.controller;

import com.testing.automation.dto.TestResult;
import com.testing.automation.model.TestPlan;
import com.testing.automation.service.TestPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.testing.automation.dto.VariableContext;

@RestController
@RequestMapping("/api/test-plans")
@CrossOrigin(origins = "*")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;

    @GetMapping
    public List<TestPlan> getAll(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return testPlanService.findByProjectId(projectId);
        }
        return testPlanService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestPlan> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testPlanService.findById(id));
    }

    @PostMapping
    public TestPlan create(@RequestBody TestPlan plan) {
        return testPlanService.create(plan);
    }

    @PutMapping("/{id}")
    public TestPlan update(@PathVariable Long id, @RequestBody TestPlan plan) {
        return testPlanService.update(id, plan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public List<TestResult> execute(@PathVariable Long id, @RequestParam String envKey) {
        return testPlanService.executePlan(id, envKey);
    }

    /**
     * Analyze variables produced/consumed in a test plan
     */
    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<Integer, VariableContext>> analyzeVariables(@PathVariable Long id) {
        return ResponseEntity.ok(testPlanService.analyzePlanVariables(id));
    }

    /**
     * Get suggested parameters for a test case
     */
    @GetMapping("/cases/{caseId}/suggest-parameters")
    public ResponseEntity<List<String>> suggestParameters(@PathVariable Long caseId) {
        return ResponseEntity.ok(testPlanService.suggestParameters(caseId));
    }

    /**
     * Save plan-specific parameter configuration for a test case
     * Only handles parameter passing, not case overrides
     */
    @PostMapping("/{id}/cases/{caseId}/parameters")
    public ResponseEntity<Void> saveCaseParameters(@PathVariable Long id, @PathVariable Long caseId,
            @RequestBody com.testing.automation.dto.TestPlanCaseOverride config) {
        testPlanService.saveCaseParameters(id, caseId, config);
        return ResponseEntity.ok().build();
    }
}
