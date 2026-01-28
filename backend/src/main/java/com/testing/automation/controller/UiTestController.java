package com.testing.automation.controller;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.testing.automation.model.UiTestCase;
import com.testing.automation.model.UiTestStep;
import com.testing.automation.model.UiTestExecutionRecord;
import com.testing.automation.model.UiTestExecutionLog;
import com.testing.automation.service.UiTestRunner;
import com.testing.automation.service.UiTestService;
import com.testing.automation.service.UiTestRecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ui-tests")
@RequiredArgsConstructor
@CrossOrigin
public class UiTestController {
    private final UiTestService uiTestService;
    private final UiTestRunner uiTestRunner;
    private final UiTestRecordingService recordingService;

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
    public UiTestExecutionRecord execute(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "server") String mode) {
        return uiTestRunner.executeCase(id, mode);
    }

    @GetMapping("/cases/{id}/local-script")
    public ResponseEntity<Map<String, Object>> getLocalScript(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            String script = uiTestRunner.generateLocalExecutionScript(id);
            result.put("success", true);
            result.put("script", script);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to generate local script for case: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
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

    // Recording APIs
    @PostMapping("/cases/{caseId}/start-recording")
    public ResponseEntity<Map<String, Object>> startRecording(
            @PathVariable Long caseId,
            @RequestBody Map<String, String> request) {
        try {
            String targetUrl = request.get("targetUrl");
            if (targetUrl == null || targetUrl.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "targetUrl is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            recordingService.startRecording(caseId, targetUrl);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Recording started");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to start recording for case: {}", caseId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/cases/{caseId}/stop-recording")
    public ResponseEntity<Map<String, Object>> stopRecording(@PathVariable Long caseId) {
        try {
            recordingService.stopRecording(caseId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Recording stopped");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to stop recording for case: {}", caseId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/cases/{caseId}/recording-code")
    public ResponseEntity<Map<String, Object>> getRecordingCode(@PathVariable Long caseId) {
        try {
            String code = recordingService.getRecordedCode(caseId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", code);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get recording code for case: {}", caseId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/cases/{caseId}/recording-status")
    public ResponseEntity<Map<String, Object>> getRecordingStatus(@PathVariable Long caseId) {
        try {
            UiTestRecordingService.RecordingStatus status = recordingService.getRecordingStatus(caseId);
            Map<String, Object> result = new HashMap<>();
            if (status != null) {
                result.put("isRecording", status.isRecording());
                result.put("startTime", status.getStartTime());
                result.put("codeLines", status.getCodeLines());
                result.put("targetUrl", status.getTargetUrl());
            } else {
                result.put("isRecording", false);
            }
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get recording status for case: {}", caseId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Local Execution APIs
    @GetMapping("/check-playwright")
    public ResponseEntity<Map<String, Object>> checkPlaywright() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Try to create Playwright instance and launch browser
            Playwright playwright = Playwright.create();
            BrowserType chromium = playwright.chromium();
            
            boolean installed = false;
            String errorMessage = null;
            
            try {
                // Try to launch browser (will trigger auto-install if not installed)
                Browser browser = chromium.launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList("--no-sandbox", "--disable-gpu")));
                browser.close();
                installed = true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                log.warn("Playwright browser not installed or failed to launch: {}", e.getMessage());
            } finally {
                playwright.close();
            }
            
            result.put("installed", installed);
            if (errorMessage != null) {
                result.put("error", errorMessage);
            }
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to check Playwright installation", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("installed", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/install-playwright")
    public ResponseEntity<Map<String, Object>> installPlaywright() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("Installing Playwright browsers...");
            
            // Playwright Java automatically downloads browsers on first launch
            // We'll trigger the download by launching each browser type
            Playwright playwright = Playwright.create();
            
            try {
                // Try to launch Chromium (most common)
                BrowserType chromium = playwright.chromium();
                Browser browser = chromium.launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList("--no-sandbox", "--disable-gpu")));
                browser.close();
                log.info("Chromium browser installed successfully");
            } catch (Exception e) {
                log.warn("Failed to install Chromium: {}", e.getMessage());
            }
            
            playwright.close();
            
            result.put("success", true);
            result.put("message", "Playwright browsers installation initiated. Browsers will be downloaded automatically on first use.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to install Playwright", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
