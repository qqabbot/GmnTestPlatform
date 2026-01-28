package com.testing.automation.controller;

import com.testing.automation.Mapper.TestExecutionLogMapper;
import com.testing.automation.Mapper.TestExecutionRecordMapper;
import com.testing.automation.model.TestExecutionLog;
import com.testing.automation.model.TestExecutionRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private TestExecutionRecordMapper executionRecordMapper;

    @Autowired
    private TestExecutionLogMapper executionLogMapper;

    @GetMapping
    public ResponseEntity<List<TestExecutionRecord>> getAllRecords() {
        return ResponseEntity.ok(executionRecordMapper.findAll());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TestExecutionRecord>> getRecordsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(executionRecordMapper.findByProjectId(projectId));
    }

    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<TestExecutionRecord>> getRecordsByModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(executionRecordMapper.findByModuleId(moduleId));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TestExecutionLog>> getRecordLogs(@PathVariable Long id) {
        return ResponseEntity.ok(executionLogMapper.findByRecordId(id));
    }
}
