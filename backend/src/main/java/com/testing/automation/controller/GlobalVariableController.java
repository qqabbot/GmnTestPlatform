package com.testing.automation.controller;

import com.testing.automation.model.GlobalVariable;
import com.testing.automation.service.GlobalVariableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/variables")
@CrossOrigin(origins = "*")
public class GlobalVariableController {

    @Autowired
    private GlobalVariableService globalVariableService;

    @GetMapping("/environment/{envId}")
    public ResponseEntity<List<GlobalVariable>> getVariablesByEnvironment(@PathVariable Long envId) {
        return ResponseEntity.ok(globalVariableService.getVariablesByEnvironmentId(envId));
    }

    @PostMapping
    public ResponseEntity<GlobalVariable> createVariable(@RequestBody GlobalVariable variable) {
        return ResponseEntity.ok(globalVariableService.createVariable(variable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalVariable> updateVariable(@PathVariable Long id, @RequestBody GlobalVariable variable) {
        GlobalVariable updated = globalVariableService.updateVariable(id, variable);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariable(@PathVariable Long id) {
        globalVariableService.deleteVariable(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/environment/{envId}/sync")
    public ResponseEntity<Void> syncVariables(@PathVariable Long envId, @RequestBody Map<String, String> variables) {
        globalVariableService.syncVariables(envId, variables);
        return ResponseEntity.ok().build();
    }
}
