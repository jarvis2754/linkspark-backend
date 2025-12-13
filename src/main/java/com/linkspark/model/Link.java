package com.linkspark.model;

import com.linkspark.domain.Team;
import com.linkspark.domain.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Column(nullable = false, columnDefinition = "int4 default 0")
    private int clicks = 0;

    @Column(nullable = false, columnDefinition = "int4 default 0")
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;


    @ElementCollection
    @CollectionTable(name = "link_week_clicks", joinColumns = @JoinColumn(name = "link_id"))
    @Column(name = "click")
    private List<Integer> weekClicks = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));
}
