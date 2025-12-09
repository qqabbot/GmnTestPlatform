package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Extractor {
    private Long id;
    private Long stepId;
    @JsonIgnoreProperties("extractors")
    private TestStep testStep;
    private String variableName;
    private String type;
    private String expression;
    private LocalDateTime createdAt = LocalDateTime.now();
}
