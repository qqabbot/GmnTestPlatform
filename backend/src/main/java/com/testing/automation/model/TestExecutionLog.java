package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TestExecutionLog {
    private Long id;
    private Long recordId;
    private String stepName;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private String variableSnapshot;
    private LocalDateTime createdAt = LocalDateTime.now();
}
