package com.linkspark.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "analytics")
@Data
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alias;

    private String ip;

    private String userAgent;

    private String referer;

    private LocalDateTime clickedAt = LocalDateTime.now();
}

