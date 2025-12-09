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
            @RequestBody(required = false) String content) {

        int count;
        if (url != null && !url.isEmpty()) {
            count = importService.importSwagger(projectId, url, true);
        } else if (content != null && !content.isEmpty()) {
            count = importService.importSwagger(projectId, content, false);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "URL or Content is required"));
        }
        return ResponseEntity.ok(Map.of("message", "Imported " + count + " paths", "count", count));
    }
}
