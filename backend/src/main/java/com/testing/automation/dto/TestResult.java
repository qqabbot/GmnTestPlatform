package com.testing.automation.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 单个用例的执行结果 (用于返回给前端实时展示)
 */
@Data
@Builder
public class TestResult {
    private Long caseId;
    private String caseName;
    private String status; // PASS, FAIL, SKIP
    private String message; // Execution message
    private int responseCode; // Same as statusCode but used in some places
    private String responseBody; // HTTP Response Body
    private java.util.Map<String, String> responseHeaders;
    private String requestUrl;
    private String method;
    private String requestBody;
    private String detail; // 失败原因或详细信息
    private Long duration; // 执行时间 (ms) - Use Long wrapper to match ScenarioExecutionEngine check
    private java.util.List<com.testing.automation.model.TestExecutionLog> logs;
}