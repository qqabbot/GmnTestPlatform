package com.testing.automation.controller;

import com.testing.automation.dto.ScenarioExecutionEvent;
import com.testing.automation.dto.TestScenarioStepDTO;
import com.testing.automation.model.TestScenario;
import com.testing.automation.model.TestScenarioStep;
import com.testing.automation.service.ScenarioExecutionEngine;
import com.testing.automation.service.TestScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/scenarios")
@CrossOrigin(origins = "*")
public class TestScenarioController {

    @Autowired
    private TestScenarioService scenarioService;

    @GetMapping
    public List<TestScenario> getAll(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return scenarioService.findByProjectId(projectId);
        }
        return scenarioService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestScenario> getById(@PathVariable Long id) {
        TestScenario scenario = scenarioService.findById(id);
        return scenario != null ? ResponseEntity.ok(scenario) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public TestScenario create(@RequestBody TestScenario scenario) {
        return scenarioService.create(scenario);
    }

    @PutMapping("/{id}")
    public TestScenario update(@PathVariable Long id, @RequestBody TestScenario scenario) {
        return scenarioService.update(id, scenario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scenarioService.delete(id);
        return ResponseEntity.ok().build();
    }

    // --- Steps Management ---

    @GetMapping("/{id}/steps/tree")
    public List<TestScenarioStepDTO> getStepsTree(@PathVariable Long id) {
        return scenarioService.getScenarioStepsTree(id);
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<Void> addStep(@PathVariable Long id, @RequestBody TestScenarioStep step) {
        step.setScenarioId(id);
        scenarioService.addStep(step);
        return ResponseEntity.ok().build(); // TODO: Return created step?
    }

    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<Void> updateStep(@PathVariable Long id, @PathVariable Long stepId,
            @RequestBody TestScenarioStep step) {
        if (!id.equals(step.getScenarioId())) {
            // Basic validation, though service handles logic
        }
        scenarioService.updateStep(stepId, step);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long id, @PathVariable Long stepId) {
        scenarioService.deleteStep(stepId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/steps/sync")
    public ResponseEntity<Void> syncSteps(@PathVariable Long id, @RequestBody List<TestScenarioStepDTO> steps) {
        scenarioService.syncSteps(id, steps);
        return ResponseEntity.ok().build();
    }

    // --- Execution ---

    @Autowired
    private ScenarioExecutionEngine executionEngine;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostMapping("/{id}/execute")
    public List<com.testing.automation.dto.TestResult> execute(@PathVariable Long id, @RequestParam String envKey) {
        return executionEngine.executeScenario(id, envKey);
    }

    @GetMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@PathVariable Long id, @RequestParam String envKey) {
        SseEmitter emitter = new SseEmitter(-1L);

        executorService.execute(() -> {
            try {
                // Send an initial event to establish connection
                emitter.send(ScenarioExecutionEvent.builder()
                        .type("info")
                        .payload("Connecting to execution engine...")
                        .timestamp(System.currentTimeMillis())
                        .build());

                executionEngine.executeScenario(id, envKey, event -> {
                    try {
                        emitter.send(event);
                    } catch (IOException e) {
                        log.warn("Failed to send SSE event: {}", e.getMessage());
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("Scenario SSE Execution Error: {}", e.getMessage(), e);
                try {
                    emitter.send(ScenarioExecutionEvent.builder()
                            .type("error")
                            .payload(e.getMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });

        return emitter;
    }
}
