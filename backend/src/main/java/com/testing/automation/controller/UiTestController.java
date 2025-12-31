package com.testing.automation.controller;

import com.testing.automation.model.UiTestCase;
import com.testing.automation.model.UiTestStep;
import com.testing.automation.model.UiTestExecutionRecord;
import com.testing.automation.model.UiTestExecutionLog;
import com.testing.automation.service.UiTestRunner;
import com.testing.automation.service.UiTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ui-tests")
@RequiredArgsConstructor
@CrossOrigin
public class UiTestController {
    private final UiTestService uiTestService;
    private final UiTestRunner uiTestRunner;

    @PostMapping("/cases")
    public UiTestCase saveCase(@RequestBody UiTestCase uiCase) {
        log.info("CONTROLLER: Saving UI Case: {}", uiCase);
        return uiTestService.saveCase(uiCase);
    }

    @GetMapping("/cases")
    public List<UiTestCase> getAllCases() {
        return uiTestService.getAllCases();
    }

    @GetMapping("/cases/{id}")
    public UiTestCase getCase(@PathVariable Long id) {
        return uiTestService.getCaseById(id);
    }

    @GetMapping("/projects/{projectId}/cases")
    public List<UiTestCase> getCasesByProject(@PathVariable Long projectId) {
        return uiTestService.getCasesByProject(projectId);
    }

    @GetMapping("/modules/{moduleId}/cases")
    public List<UiTestCase> getCasesByModule(@PathVariable Long moduleId) {
        return uiTestService.getCasesByModule(moduleId);
    }

    @DeleteMapping("/cases/{id}")
    public void deleteCase(@PathVariable Long id) {
        uiTestService.deleteCase(id);
    }

    @PostMapping("/steps")
    public void saveStep(@RequestBody UiTestStep step) {
        uiTestService.saveStep(step);
    }

    @GetMapping("/cases/{caseId}/steps")
    public List<UiTestStep> getSteps(@PathVariable Long caseId) {
        return uiTestService.getStepsByCase(caseId);
    }

    @DeleteMapping("/steps/{id}")
    public void deleteStep(@PathVariable Long id) {
        uiTestService.deleteStep(id);
    }

    @PostMapping("/cases/{id}/execute")
    public UiTestExecutionRecord execute(@PathVariable Long id) {
        return uiTestRunner.executeCase(id);
    }

    // Reports
    @GetMapping("/cases/{caseId}/records")
    public List<UiTestExecutionRecord> getRecords(@PathVariable Long caseId) {
        return uiTestService.getRecordsByCase(caseId);
    }

    @GetMapping("/projects/{projectId}/records")
    public List<UiTestExecutionRecord> getRecordsByProject(@PathVariable Long projectId) {
        return uiTestService.getRecordsByProject(projectId);
    }

    @GetMapping("/records")
    public List<UiTestExecutionRecord> getAllRecords() {
        return uiTestService.getAllRecords();
    }

    @GetMapping("/records/{id}")
    public UiTestExecutionRecord getRecord(@PathVariable Long id) {
        return uiTestService.getRecordById(id);
    }

    @GetMapping("/records/{recordId}/logs")
    public List<UiTestExecutionLog> getLogs(@PathVariable Long recordId) {
        return uiTestService.getLogsByRecord(recordId);
    }
}
