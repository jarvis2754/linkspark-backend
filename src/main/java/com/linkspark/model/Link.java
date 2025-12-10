package com.linkspark.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "links")
@Data
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String originalUrl;

    @Column(unique = true, nullable = false)
    private String alias;

    private String tags;

    private LocalDateTime expiresAt;

    private boolean passwordProtect;

    private String passwordHash;

    private boolean enableAnalytics;

    @Column(nullable = false)
    private int redirectType;

    private LocalDateTime createdAt = LocalDateTime.now();
}

