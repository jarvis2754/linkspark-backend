package com.linkspark.controller;

import com.linkspark.model.Link;
import com.linkspark.service.AnalyticsService;
import com.linkspark.service.LinkService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;
    private final AnalyticsService analyticsService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/{alias}")
    public void redirect(
            @PathVariable("alias") String alias,
            @RequestParam(required = false) String password,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws Exception {

        Link link = linkService.getLink(alias);

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.sendError(410, "Link expired");
            return;
        }

        if (link.isPasswordProtect()) {
            if (password == null || !passwordEncoder.matches(password, link.getPasswordHash())) {
                response.sendError(401, "Password required");
                return;
            }
        }

        if (link.isEnableAnalytics()) {
            analyticsService.recordHit(alias, request);
        }

        response.setHeader("Location", link.getOriginalUrl());
        response.setStatus(link.getRedirectType());
        response.flushBuffer();
    }
}
