package com.linkspark.controller;

import com.linkspark.dto.AnalyticsResponse;
import com.linkspark.service.AnalyticsService;
import com.linkspark.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final LinkService linkService;

    @GetMapping("/id/{id}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        var link = linkService.getOneLink(id);
        String alias = link.getAlias();

        AnalyticsResponse resp = analyticsService.getMetricsForAlias(alias, range, start, end);
        return ResponseEntity.ok(resp);
    }

}

