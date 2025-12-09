package com.testing.automation.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DryRunResponse {
    private String resolvedUrl;
    private Map<String, String> resolvedHeaders;
    private String resolvedBody;
    private Map<String, Object> variables;
}
