package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class GlobalVariable {
    private Long id;
    private String keyName;
    private String valueContent;
    private String type = "normal";
    private String description;
    private Long environmentId;
    private Long projectId;
    private Long moduleId;
    @JsonIgnoreProperties("hibernateLazyInitializer")
    private Environment environment;
    @JsonIgnoreProperties("modules")
    private Project project;
    @JsonIgnoreProperties("project")
    private TestModule module;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
