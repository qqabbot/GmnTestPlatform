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
    private String parameterOverrides;
    private String caseNameOverride;
    private String urlOverride;
    private String methodOverride;
    private String headersOverride;
    private String bodyOverride;
    private String assertionScriptOverride;
    private String stepsOverride;
    private Boolean planEnabled;

    // Helper methods to get effective values (considering plan overrides)
    public String getEffectiveCaseName() {
        return caseNameOverride != null ? caseNameOverride : caseName;
    }

    public String getEffectiveUrl() {
        return urlOverride != null ? urlOverride : url;
    }

    public String getEffectiveMethod() {
        return methodOverride != null ? methodOverride : method;
    }

    public String getEffectiveHeaders() {
        return headersOverride != null ? headersOverride : headers;
    }

    public String getEffectiveBody() {
        return bodyOverride != null ? bodyOverride : body;
    }

    public String getEffectiveAssertionScript() {
        return assertionScriptOverride != null ? assertionScriptOverride : assertionScript;
    }

    /**
     * Get effective steps by merging original steps with overrides
     */
    public List<TestStep> getEffectiveSteps() {
        if (stepsOverride == null || stepsOverride.trim().isEmpty() || stepsOverride.equals("[]")) {
            return steps;
        }

        try {
            List<Map<String, Object>> overrides = mapper.readValue(stepsOverride,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // Create a deep copy of steps to avoid mutating the original template
            List<TestStep> effectiveSteps = new ArrayList<>();
            for (TestStep original : steps) {
                TestStep effective = new TestStep();
                // Copy basic fields (manually or via a utility if available)
                effective.setId(original.getId());
                effective.setCaseId(original.getCaseId());
                effective.setStepOrder(original.getStepOrder());
                effective.setStepName(original.getStepName());
                effective.setMethod(original.getMethod());
                effective.setUrl(original.getUrl());
                effective.setHeaders(original.getHeaders());
                effective.setBody(original.getBody());
                effective.setAuthType(original.getAuthType());
                effective.setAuthValue(original.getAuthValue());
                effective.setAssertionScript(original.getAssertionScript());
                effective.setEnabled(original.getEnabled());
                effective.setReferenceCaseId(original.getReferenceCaseId());
                effective.setReferenceCase(original.getReferenceCase());
                effective.setExtractors(original.getExtractors());
                effective.setAssertions(original.getAssertions());

                // Apply override if step ID matches
                for (Map<String, Object> override : overrides) {
                    Object overrideStepIdStr = override.get("stepId");
                    if (overrideStepIdStr == null)
                        continue;

                    Long overrideStepId = Long.valueOf(overrideStepIdStr.toString());
                    if (overrideStepId.equals(original.getId())) {
                        if (override.containsKey("url"))
                            effective.setUrl((String) override.get("url"));
                        if (override.containsKey("method"))
                            effective.setMethod((String) override.get("method"));
                        if (override.containsKey("body"))
                            effective.setBody((String) override.get("body"));
                        if (override.containsKey("assertionScript"))
                            effective.setAssertionScript((String) override.get("assertionScript"));
                        if (override.containsKey("enabled"))
                            effective.setEnabled((Boolean) override.get("enabled"));
                    }
                }
                effectiveSteps.add(effective);
            }
            return effectiveSteps;
        } catch (Exception e) {
            log.error("Failed to parse stepsOverride JSON: " + stepsOverride, e);
            return steps;
        }
    }
}