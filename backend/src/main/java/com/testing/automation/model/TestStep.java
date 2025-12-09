package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TestStep {
    private Long id;
    private Long caseId; // FK
    @JsonIgnoreProperties({ "steps" })
    private TestCase testCase; // For Jackson only
    private Integer stepOrder;
    private String stepName;
    private String method;
    private String url;
    private String headers;
    private String body;
    private String authType;
    private String authValue;
    private String assertionScript;
    private Boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private List<Extractor> extractors = new ArrayList<>();
    private List<Assertion> assertions = new ArrayList<>();
}
