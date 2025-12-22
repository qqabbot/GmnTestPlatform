package com.testing.automation.dto;

import lombok.Data;

@Data
public class AiGenerateRequest {
    private String url;
    private String method;
    private String headers;
    private String body;
    private Integer count = 5; // 默认生成 5 个
}
