package com.testing.automation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TestPlan {
    private Long id;
    private String name;
    private String description;
    private Long projectId;
    @JsonIgnoreProperties("modules")
    private Project project;
    @JsonIgnoreProperties({ "module", "steps" })
    private List<TestCase> testCases = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
