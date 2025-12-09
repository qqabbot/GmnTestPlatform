package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestStepTemplate {
    private Long id;
    private String name;
    private String method;
    private String url;
    private String headers;
    private String body;
    private String assertionScript;
    private Long projectId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
