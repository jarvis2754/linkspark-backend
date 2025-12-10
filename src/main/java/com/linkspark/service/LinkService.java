package com.linkspark.service;

import com.linkspark.dto.CreateLinkRequest;
import com.linkspark.model.Link;
import com.linkspark.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepo;
    private final PasswordEncoder passwordEncoder;

    public String generateRandomAlias() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    public String createLink(CreateLinkRequest req) {
        String alias = (req.getCustomAlias() == null || req.getCustomAlias().isBlank())
                ? generateRandomAlias()
                : req.getCustomAlias();

        if (linkRepo.existsByAlias(alias)) {
            throw new RuntimeException("Alias already taken");
        }

        Link link = new Link();
        link.setTitle(req.getTitle());
        link.setOriginalUrl(req.getOriginalUrl());
        link.setAlias(alias);
        link.setTags(req.getTags());
        link.setEnableAnalytics(req.isEnableAnalytics());
        link.setRedirectType(Integer.parseInt(req.getRedirectType()));

        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            link.setExpiresAt(LocalDateTime.parse(req.getExpiresAt()));
        }

        if (req.isPasswordProtect()) {
            link.setPasswordProtect(true);
            link.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }

        linkRepo.save(link);
        return alias; // <-- return String alias
    }

    public boolean isAliasAvailable(String alias) {
        return !linkRepo.existsByAlias(alias);
    }

    public Link getLink(String alias) {
        return linkRepo.findByAlias(alias)
                .orElseThrow(() -> new RuntimeException("Link not found"));
    }
}

