package com.linkspark.controller;

import com.linkspark.dto.CreateLinkRequest;
import com.linkspark.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateLinkRequest req) {
        String alias = linkService.createLink(req);

        // Return local development URL
        return ResponseEntity.ok(
                Map.of("shortUrl", "http://localhost:8000/" + alias)
        );
    }

    @GetMapping("/check-alias")
    public ResponseEntity<?> checkAlias(@RequestParam("alias") String alias) {
        boolean available = linkService.isAliasAvailable(alias);
        return ResponseEntity.ok(Map.of("available", available));
    }
}
