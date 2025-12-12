package com.linkspark.controller;

import com.linkspark.dto.DashboardResponse;
import com.linkspark.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard(Authentication auth) {
        return dashboardService.buildDashboard(auth);
    }
}

