package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestScenarioStep {
    private Long id;
    private Long scenarioId;
    private Long parentId;
    private String type; // 'CASE', 'GROUP', 'LOOP', 'IF', 'WAIT', 'SCRIPT'
    private String name;

    private Long referenceCaseId;
    private Long referenceScenarioId; // For nested scenario reference

    // Stored as JSON strings in DB
    private String controlLogic;
    private String dataOverrides;

    private Integer orderIndex;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Transient fields for hierarchy processing (not in DB)
    // private List<TestScenarioStep> children; // Managed by DTO usually
}
