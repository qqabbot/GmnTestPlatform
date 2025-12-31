package com.testing.automation.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UiTestCase {
    private Long id;
    private Long projectId;
    private Long moduleId;
    private String name;
    private String description;
    private String startUrl;
    private String browserType; // chromium, firefox, webkit
    private Boolean headless;
    private Integer viewportWidth;
    private Integer viewportHeight;
    private String customHeaders; // JSON format: {"x-token": "xxx", "Authorization": "Bearer xxx"}
    private String customCookies; // JSON format: [{"name": "foo", "value": "bar", "domain": "example.com",
                                  // "path": "/"}]
    private Boolean autoDismissDialogs; // Automatically dismiss alert/confirm/prompt dialogs
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UiTestStep> steps;
}
