package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestModule {
    private Long id;
    private String moduleName;
    private Long projectId; // Store FK instead of object
    @JsonIgnoreProperties({ "modules" })
    private Project project; // For Jackson serialization only
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}