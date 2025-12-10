package com.linkspark.service;

import com.linkspark.model.Analytics;
import com.linkspark.repository.AnalyticsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository repo;

    public void recordHit(String alias, HttpServletRequest request) {
        Analytics a = new Analytics();
        a.setAlias(alias);
        a.setIp(request.getRemoteAddr());
        a.setUserAgent(request.getHeader("User-Agent"));
        a.setReferer(request.getHeader("Referer"));
        repo.save(a);
    }
}

