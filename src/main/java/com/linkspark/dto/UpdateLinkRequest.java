package com.linkspark.dto;

import lombok.Data;

@Data
public class UpdateLinkRequest {
    private String title;
    private String tags;
    private boolean passwordProtect;
    private String password;
    private String expiresAt;
    private boolean enableAnalytics;
    private Integer redirectType;
}

