package com.linkspark.controller;

import com.linkspark.dto.CreateLinkRequest;
import com.linkspark.dto.LinkDto;
import com.linkspark.dto.UpdateLinkRequest;
import com.linkspark.model.Link;
import com.linkspark.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateLinkRequest req) {
        String alias = linkService.createLink(req);
        return ResponseEntity.ok(Map.of("shortUrl", "http://localhost:8000/" + alias));
    }

    @GetMapping("/check-alias")
    public ResponseEntity<?> checkAlias(@RequestParam("alias") String alias) {
        return ResponseEntity.ok(Map.of("available", linkService.isAliasAvailable(alias)));
    }

    @GetMapping
    public ResponseEntity<List<LinkDto>> getAll() {
        return ResponseEntity.ok(linkService.getAllLinks());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<LinkDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(linkService.getOneLink(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        linkService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<LinkDto> update(
            @PathVariable Long id,
            @RequestBody UpdateLinkRequest req
    ) {
        return ResponseEntity.ok(linkService.update(id, req));
    }

    @GetMapping("/alias/{alias}")
    public ResponseEntity<?> getAliasInfo(@PathVariable String alias) {
        Link link = linkService.getLinkByAlias(alias);

        return ResponseEntity.ok(Map.of(
                "alias", link.getAlias(),
                "title", link.getTitle(),
                "passwordProtected", link.isPasswordProtect(),
                "lockedSeconds", linkService.getRemainingLockSeconds(link)
        ));
    }

    @GetMapping("/alias/{alias}/status")
    public ResponseEntity<?> getStatus(@PathVariable String alias) {
        Link link = linkService.getLinkByAlias(alias);

        return ResponseEntity.ok(Map.of(
                "passwordProtected", link.isPasswordProtect(),
                "lockedSeconds", linkService.getRemainingLockSeconds(link)
        ));
    }

    @PostMapping("/{alias}/verify")
    public ResponseEntity<?> verify(
            @PathVariable String alias,
            @RequestBody Map<String, String> body
    ) {
        String password = body.get("password");

        if (password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password required"));
        }

        String token = linkService.verifyPasswordAndCreateToken(alias, password, 300);

        if (token != null) {
            return ResponseEntity.ok(Map.of("ok", true, "token", token));
        }

        Link link = linkService.getLinkByAlias(alias);
        long remaining = linkService.getRemainingLockSeconds(link);

        if (remaining > 0) {
            return ResponseEntity.status(423)
                    .body(Map.of("ok", false, "locked", true, "lockedSeconds", remaining));
        }

        return ResponseEntity.status(401)
                .body(Map.of("ok", false, "message", "Invalid password"));
    }
}
