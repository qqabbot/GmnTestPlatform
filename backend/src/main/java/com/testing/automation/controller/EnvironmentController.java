package com.testing.automation.controller;

import com.testing.automation.model.Environment;
import com.testing.automation.service.EnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/environments")
@CrossOrigin(origins = "*")
public class EnvironmentController {

    @Autowired
    private EnvironmentService environmentService;

    @GetMapping
    public ResponseEntity<List<Environment>> getAllEnvironments() {
        return ResponseEntity.ok(environmentService.getAllEnvironments());
    }

    @PostMapping
    public ResponseEntity<Environment> createEnvironment(@RequestBody Environment environment) {
        return ResponseEntity.ok(environmentService.createEnvironment(environment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Environment> getEnvironmentById(@PathVariable Long id) {
        Environment env = environmentService.getEnvironmentById(id);
        if (env != null) {
            return ResponseEntity.ok(env);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Environment> updateEnvironment(@PathVariable Long id, @RequestBody Environment environment) {
        Environment updatedEnv = environmentService.updateEnvironment(id, environment);
        if (updatedEnv != null) {
            return ResponseEntity.ok(updatedEnv);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id) {
        environmentService.deleteEnvironment(id);
        return ResponseEntity.ok().build();
    }
}
