package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ScenarioExecutionRecord {
    private Long id;
    private Long scenarioId;
    private String scenarioName;
    private String envKey;
    private String status; // PASS/FAIL/PARTIAL/RUNNING
    private Integer totalSteps;
    private Integer passedSteps;
    private Integer failedSteps;
    private Long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
