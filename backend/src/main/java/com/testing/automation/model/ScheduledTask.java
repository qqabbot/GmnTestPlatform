package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ScheduledTask {
    private Long id;
    private String name;
    private String cronExpression;
    private Long planId;
    private String envKey;
    private String status = "ACTIVE"; // ACTIVE or PAUSED
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime createdAt = LocalDateTime.now();
}
