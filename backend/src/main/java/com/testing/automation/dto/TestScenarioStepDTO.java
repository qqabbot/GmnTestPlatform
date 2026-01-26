package com.testing.automation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TestScenarioStepDTO {
    private Long id;
    private Long scenarioId;
    private Long parentId;
    private String type;
    private String name;

    private Long referenceCaseId;
    private Long referenceScenarioId; // For nested scenario reference

    // Parsed JSON objects
    private Object controlLogic; // e.g., Map or specific DTO
    private Object dataOverrides;

    private Integer orderIndex;

    // Recursive children for Tree View
    private List<TestScenarioStepDTO> children = new ArrayList<>();
}
