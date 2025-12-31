package com.testing.automation.dto;

import lombok.Data;

/**
 * DTO for Test Plan specific overrides of a Test Case
 * Stores plan-specific modifications without polluting original test_case table
 */
@Data
public class TestPlanCaseOverride {
    private Long planId;
    private Long caseId;

    // Case-level overrides
    private String caseNameOverride;
    private String urlOverride;
    private String methodOverride;
    private String headersOverride;
    private String bodyOverride;
    private String assertionScriptOverride;

    // Step-level overrides (JSON array)
    private String stepsOverride;

    // Parameter overrides (existing field)
    private String parameterOverrides;

    // Enable/disable case in this plan
    private Boolean enabled;
}
