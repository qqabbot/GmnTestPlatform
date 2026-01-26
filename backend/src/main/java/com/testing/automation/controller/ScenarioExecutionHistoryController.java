package com.testing.automation.controller;

import com.testing.automation.model.ScenarioExecutionRecord;
import com.testing.automation.model.ScenarioStepExecutionLog;
import com.testing.automation.Mapper.ScenarioExecutionRecordMapper;
import com.testing.automation.Mapper.ScenarioStepExecutionLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
@CrossOrigin(origins = "*")
public class ScenarioExecutionHistoryController {

    @Autowired
    private ScenarioExecutionRecordMapper executionRecordMapper;

    @Autowired
    private ScenarioStepExecutionLogMapper stepLogMapper;

    /**
     * Get execution history for a scenario
     */
    @GetMapping("/{id}/history")
    public List<ScenarioExecutionRecord> getExecutionHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        return executionRecordMapper.findByScenarioId(id, limit);
    }

    /**
     * Get execution history filtered by environment
     */
    @GetMapping("/{id}/history/env/{envKey}")
    public List<ScenarioExecutionRecord> getExecutionHistoryByEnv(
            @PathVariable Long id,
            @PathVariable String envKey,
            @RequestParam(defaultValue = "10") int limit) {
        return executionRecordMapper.findByScenarioIdAndEnv(id, envKey, limit);
    }

    /**
     * Get detailed step logs for a specific execution record
     */
    @GetMapping("/executions/{recordId}/logs")
    public List<ScenarioStepExecutionLog> getExecutionLogs(@PathVariable Long recordId) {
        return stepLogMapper.findByRecordId(recordId);
    }

    /**
     * Get a specific execution record
     */
    @GetMapping("/executions/{recordId}")
    public ResponseEntity<ScenarioExecutionRecord> getExecutionRecord(@PathVariable Long recordId) {
        ScenarioExecutionRecord record = executionRecordMapper.findById(recordId);
        return record != null ? ResponseEntity.ok(record) : ResponseEntity.notFound().build();
    }

    /**
     * Delete an execution record (and its logs via cascade)
     */
    @DeleteMapping("/executions/{recordId}")
    public ResponseEntity<Void> deleteExecutionRecord(@PathVariable Long recordId) {
        executionRecordMapper.deleteById(recordId);
        return ResponseEntity.ok().build();
    }
}
