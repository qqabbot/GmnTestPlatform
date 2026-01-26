package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ScenarioStepExecutionLog {
    private Long id;
    private Long recordId;
    private Long stepId;
    private String stepName;
    private String stepType;
    private String status; // PASS/FAIL/SKIP
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestBody;
    private Integer responseCode;
    private String responseHeaders;
    private String responseBody;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime executedAt;
}
