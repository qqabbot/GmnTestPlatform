package com.testing.automation.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UiTestExecutionRecord {
    private Long id;
    private Long projectId;
    private Long caseId;
    private String caseName;
    private String status;
    private String videoPath;
    private Long duration;
    private String errorMessage;
    private LocalDateTime executedAt;
}
