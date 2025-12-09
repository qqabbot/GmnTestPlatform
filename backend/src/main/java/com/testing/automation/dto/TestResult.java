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
    private int statusCode; // HTTP Status Code
    private String responseBody; // HTTP Response Body
    private String detail; // 失败原因或详细信息
    private long duration; // 执行时间 (ms)
    private java.util.List<com.testing.automation.model.TestExecutionLog> logs;
}