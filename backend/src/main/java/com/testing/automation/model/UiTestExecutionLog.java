package com.testing.automation.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UiTestExecutionLog {
    private Long id;
    private Long recordId;
    private Long stepId;
    private String stepName;
    private String action;
    private String selector; // Element selector that was interacted with
    private String status;
    private String screenshotPath;
    private String errorDetail;
    private LocalDateTime executedAt;
}
