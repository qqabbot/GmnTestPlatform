package com.testing.automation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioExecutionEvent {
    private String type; // step_start, step_complete, scenario_start, scenario_complete
    private Long stepId;
    private String stepName;
    private String status;
    private Object payload;
    private TestResult result;
    private java.util.Map<String, Object> variables;
    private long timestamp;
}
