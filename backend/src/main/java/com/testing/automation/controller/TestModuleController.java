package com.testing.automation.controller;

import com.testing.automation.model.TestModule;
import com.testing.automation.service.TestModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@CrossOrigin(origins = "*")
public class TestModuleController {

    @Autowired
    private TestModuleService testModuleService;

    @GetMapping
    public ResponseEntity<List<TestModule>> getAllModules(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return ResponseEntity.ok(testModuleService.getModulesByProjectId(projectId));
        }
        return ResponseEntity.ok(testModuleService.getAllModules());
    }

    @PostMapping
    public ResponseEntity<TestModule> createModule(@RequestBody TestModule module) {
        return ResponseEntity.ok(testModuleService.createModule(module));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestModule> getModuleById(@PathVariable Long id) {
        TestModule module = testModuleService.getModuleById(id);
        if (module != null) {
            return ResponseEntity.ok(module);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        testModuleService.deleteModule(id);
        return ResponseEntity.ok().build();
    }
}
