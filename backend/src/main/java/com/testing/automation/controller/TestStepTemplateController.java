package com.testing.automation.controller;

import com.testing.automation.model.TestStepTemplate;
import com.testing.automation.service.TestStepTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/step-templates")
@CrossOrigin(origins = "*")
public class TestStepTemplateController {

    @Autowired
    private TestStepTemplateService service;

    @GetMapping
    public ResponseEntity<List<TestStepTemplate>> getAll(@RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(service.findAll(projectId));
    }

    @PostMapping
    public ResponseEntity<TestStepTemplate> create(@RequestBody TestStepTemplate template) {
        return ResponseEntity.ok(service.create(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestStepTemplate> update(@PathVariable Long id, @RequestBody TestStepTemplate template) {
        return ResponseEntity.ok(service.update(id, template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
