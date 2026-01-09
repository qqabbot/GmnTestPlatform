package com.testing.automation.dto;

import lombok.Data;

/**
 * DTO for Test Plan specific parameter configuration
 * Simplified: Only handles parameter passing between cases
 */
@Data
public class TestPlanCaseOverride {
    private Long planId;
    private Long caseId;

    // Parameter overrides (JSON for parameter mapping)
    private String parameterOverrides;

    // Logic (assertion/extractor) override
    private String assertionScriptOverride;

    // Enable/disable case in this plan
    private Boolean enabled;
}
