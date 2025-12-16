package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("variableName")
    private String variableName;
    
    @JsonProperty("variable")  // Also accept "variable" from frontend
    public void setVariable(String variable) {
        if (this.variableName == null || this.variableName.isEmpty()) {
            this.variableName = variable;
        }
    }
    
    private String type;
    
    @JsonProperty("source")  // Also accept "source" from frontend, map to type
    public void setSource(String source) {
        if (this.type == null || this.type.isEmpty()) {
            if ("json".equalsIgnoreCase(source)) {
                this.type = "JSONPATH";
            } else if ("header".equalsIgnoreCase(source)) {
                this.type = "HEADER";
            } else {
                this.type = "JSONPATH"; // default
            }
        }
    }
    
    private String expression;
    private LocalDateTime createdAt = LocalDateTime.now();
}
