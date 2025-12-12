package com.linkspark.controller;

import com.linkspark.dto.UserAnalyticsResponse;
import com.linkspark.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    @GetMapping("/user")
    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                userAnalyticsService.getUserMetrics(auth, range, start, end)
        );
    }
}

