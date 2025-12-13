package com.linkspark.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateLinkRequest {
    private String title;
    private String originalUrl;
    private String customAlias;
    private String tags;
    private String expiresAt;
    private boolean passwordProtect;
    private String password;
    private boolean enableAnalytics;
    private String redirectType;
    private UUID teamId;
}

