package com.linkspark.repository;

import com.linkspark.model.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    List<Analytics> findByAliasOrderByClickedAtDesc(String alias);
    List<Analytics> findByAliasAndClickedAtAfter(String alias, LocalDateTime after);
}

