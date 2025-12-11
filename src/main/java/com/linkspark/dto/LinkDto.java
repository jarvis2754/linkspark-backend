package com.linkspark.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LinkDto {
    private Long id;
    private String title;
    private String shortUrl;
    private String originalUrl;
    private int clicks;
    private List<Integer> weekClicks;
    private boolean passwordProtected;
    private LocalDateTime expiresAt;
    private boolean enableAnalytics;
    private String alias;
}

