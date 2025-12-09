package com.testing.automation.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * HTTP 响应对象
 */
@Data
@Builder
public class TestResponse {
    private int statusCode; // HTTP 状态码
    private String body; // Response body
    private Map<String, Object> json; // JSON 响应体 (已解析)
    private String text; // 原始响应体
    private Map<String, String> headers; // 响应头
    private Long responseTime; // 响应时间 (毫秒)

    // Request details for logging
    private String requestUrl;
    private String requestMethod;
    private String requestBody;
    private Map<String, String> requestHeaders;
}