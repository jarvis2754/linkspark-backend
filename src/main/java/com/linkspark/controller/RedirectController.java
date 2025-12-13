package com.linkspark.controller;

import com.linkspark.model.Link;
import com.linkspark.service.AnalyticsService;
import com.linkspark.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;
    private final AnalyticsService analyticsService;

    @GetMapping("/{alias}")
    public ResponseEntity<?> handleRedirect(
            @PathVariable String alias,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request
    ) {
        Link link = linkService.getLinkByAlias(alias);

        if (link.getExpiresAt() != null &&
                link.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(410).body("Link expired");
        }

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

        analyticsService.recordHit(alias, request);
        linkService.registerClick(link);

        return ResponseEntity.status(302)
                .header("Location", link.getOriginalUrl())
                .build();
    }
}
