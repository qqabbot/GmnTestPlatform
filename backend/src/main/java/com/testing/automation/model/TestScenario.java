package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestScenario {
    private Long id;
    private String name;
    private String description;
    private Long projectId;

    @JsonIgnoreProperties("modules")
    private Project project;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Note: Steps are usually loaded separately or via DTO to avoid deep nesting
    // issues
    // but for simple mapping we might add a transient field later if needed.
}
