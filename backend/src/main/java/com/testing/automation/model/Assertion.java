package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Assertion {
    private Long id;
    private Long stepId;
    @JsonIgnoreProperties("assertions")
    private TestStep testStep;
    private String type;
    private String expression;
    private String expectedValue;
    private LocalDateTime createdAt = LocalDateTime.now();
}
