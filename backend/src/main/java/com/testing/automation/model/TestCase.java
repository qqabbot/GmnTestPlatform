package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TestCase {
    private Long id;
    private Long moduleId; // FK
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private TestModule module; // For Jackson only
    private String caseName;
    private String method;
    private String url;
    private String headers;
    private String body;
    private String precondition;
    private String setupScript;
    private String assertionScript;
    private Boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @JsonIgnoreProperties("testCase")
    private List<TestStep> steps = new ArrayList<>();

    // Transient field for Test Plan execution context (not stored in test_case
    // table)
    private String parameterOverrides;
}