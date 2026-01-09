package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
public class TestCase {
    private static final ObjectMapper mapper = new ObjectMapper();
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

    // Transient fields for Test Plan execution context (not stored in test_case
    // table)
    private String parameterOverrides; // JSON for parameter mapping
    private String assertionScriptOverride; // Logic (assertion/extractor) override
    private Boolean planEnabled; // Enable/disable in plan
    private Integer caseOrder; // Execution order in plan

    // Helper methods to get effective values (considering plan overrides)
    public String getEffectiveAssertionScript() {
        return assertionScriptOverride != null ? assertionScriptOverride : assertionScript;
    }
}