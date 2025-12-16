package com.testing.automation.controller;

import com.testing.automation.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    @Autowired
    private ImportService importService;

    @PostMapping("/swagger")
    public ResponseEntity<Map<String, Object>> importSwagger(
            @RequestParam Long projectId,
            @RequestParam(required = false) String url,
            @RequestParam(required = false, defaultValue = "false") boolean clearData,
            @RequestBody(required = false) String content) {

        int count;
        if (url != null && !url.isEmpty()) {
            count = importService.importSwagger(projectId, url, true, clearData);
        } else if (content != null && !content.isEmpty()) {
            count = importService.importSwagger(projectId, content, false, clearData);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "URL or Content is required"));
        }
        return ResponseEntity.ok(Map.of("message", "Imported " + count + " paths", "count", count));
    }

    @PostMapping("/curl")
    public ResponseEntity<Map<String, Object>> importCurl(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false, defaultValue = "false") boolean asStep,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false, defaultValue = "false") boolean parseOnly,
            @RequestBody String curlCommand) {

        try {
            // If parseOnly is true, just parse and return the data without creating records
            // projectId and moduleId are not required for parsing
            if (parseOnly) {
                var parseResult = importService.parseCurl(curlCommand);
                return ResponseEntity.ok(Map.of(
                    "message", "Successfully parsed cURL command",
                    "method", parseResult.getMethod(),
                    "url", parseResult.getUrl(),
                    "headers", parseResult.getHeadersAsJson(),
                    "body", parseResult.getBody() != null ? parseResult.getBody() : ""
                ));
            }
            
            // For non-parseOnly mode, projectId is required
            if (projectId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "projectId is required when creating a test case"
                ));
            }
            
            // Otherwise, create the test case or step
            Object result = importService.importFromCurl(projectId, moduleId, curlCommand, asStep, caseId);
            String type = asStep ? "step" : "case";
            return ResponseEntity.ok(Map.of(
                "message", "Successfully imported " + type + " from cURL",
                "type", type,
                "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to import from cURL: " + e.getMessage()
            ));
        }
    }
}
