package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestExecutionRecord {
    private Long id;
    private Long projectId;
    private Long moduleId;
    private Long caseId;
    private String caseName;
    private String envKey;
    private String status; // PASS, FAIL, ERROR
    private String detail;
    private Long duration; // ms
    private LocalDateTime executedAt = LocalDateTime.now();
}
