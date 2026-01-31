package com.testing.automation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NavAddress {

    private Long id;
    private Long environmentId;
    private String shortName;
    private String label;
    private String url;
    private String remark;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
