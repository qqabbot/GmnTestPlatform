package com.testing.automation.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UiTestStep {
    private Long id;
    private Long caseId;
    private Integer stepOrder;
    private String actionType; // NAVIGATE, CLICK, FILL, HOVER, etc.
    private String selector;
    private String value;
    private String waitCondition;
    // Transient fields for hierarchy saving
    private String tempId;
    private String parentTempId;

    private Long parentId;
    private String conditionExpression;
    private String loopSource;
    private Boolean screenshotOnFailure;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
