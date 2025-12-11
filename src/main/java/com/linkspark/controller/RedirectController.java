package com.linkspark.controller;

import com.linkspark.model.Link;
import com.linkspark.service.AnalyticsService;
import com.linkspark.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;
    private final AnalyticsService analyticsService; // ✅ ADD THIS

    @GetMapping("/{alias}")
    public ResponseEntity<?> handleRedirect(
            @PathVariable String alias,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request   // ✅ REQUIRED FOR analytics
    ) {
        Link link = linkService.getLinkByAlias(alias);

        // ------------------------
        // 1) EXPIRY CHECK
        // ------------------------
        if (link.getExpiresAt() != null &&
                link.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410).body("Link expired");
        }

        // ------------------------
        // 2) PASSWORD PROTECTION
        // ------------------------
        if (link.isPasswordProtect()) {
            if (token == null) {
                return ResponseEntity.status(302)
                        .header("Location", "http://localhost:3000/open/" + alias)
                        .build();
            }

            boolean ok = linkService.validateAndConsumeTempToken(token, alias);
            if (!ok) {
                return ResponseEntity.status(302)
                        .header("Location", "http://localhost:3000/open/" + alias)
                        .build();
            }
        }

        // ------------------------
        // 3) RECORD ANALYTICS (BEFORE REDIRECT)
        // ------------------------
        analyticsService.recordHit(alias, request);

        // ------------------------
        // 4) INCREMENT CLICK COUNT
        // ------------------------
        linkService.registerClick(link);

        // ------------------------
        // 5) REDIRECT TO DESTINATION
        // ------------------------
        return ResponseEntity.status(302)
                .header("Location", link.getOriginalUrl())
                .build();
    }
}
