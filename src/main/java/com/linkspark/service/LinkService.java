package com.linkspark.service;

import com.linkspark.dto.CreateLinkRequest;
import com.linkspark.dto.LinkDto;
import com.linkspark.dto.UpdateLinkRequest;
import com.linkspark.model.Link;
import com.linkspark.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepo;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final Map<String, TempToken> tempTokens = new ConcurrentHashMap<>();

    private static class TempToken {
        String alias;
        LocalDateTime expiresAt;
        TempToken(String alias, LocalDateTime expiresAt) {
            this.alias = alias;
            this.expiresAt = expiresAt;
        }
    }

    public String generateRandomAlias() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    @Transactional
    public String createLink(CreateLinkRequest req) {

        String alias = (req.getCustomAlias() == null || req.getCustomAlias().isBlank())
                ? generateRandomAlias()
                : req.getCustomAlias();

        if (linkRepo.existsByAlias(alias)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alias already taken");
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
        return alias;
    }

    public boolean isAliasAvailable(String alias) {
        return !linkRepo.existsByAlias(alias);
    }

    public Link getLinkByAlias(String alias) {
        return linkRepo.findByAlias(alias)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found")
                );
    }

    public List<LinkDto> getAllLinks() {
        return linkRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LinkDto getOneLink(Long id) {
        Link link = linkRepo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found")
                );
        return toDto(link);
    }

    @Transactional
    public void delete(Long id) {
        if (!linkRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found");
        }
        linkRepo.deleteById(id);
    }

    @Transactional
    public LinkDto update(Long id, UpdateLinkRequest req) {
        Link link = linkRepo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found")
                );

        if (req.getTitle() != null) link.setTitle(req.getTitle());
        link.setTags(req.getTags());
        link.setEnableAnalytics(req.isEnableAnalytics());

        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            link.setExpiresAt(LocalDateTime.parse(req.getExpiresAt()));
        } else {
            link.setExpiresAt(null);
        }

        if (req.isPasswordProtect()) {
            link.setPasswordProtect(true);
            if (req.getPassword() != null && !req.getPassword().isBlank()) {
                link.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            }
        } else {
            link.setPasswordProtect(false);
            link.setPasswordHash(null);
        }

        if (req.getRedirectType() != null) {
            link.setRedirectType(req.getRedirectType());
        }

        linkRepo.save(link);
        return toDto(link);
    }

    private LinkDto toDto(Link link) {
        LinkDto d = new LinkDto();
        d.setId(link.getId());
        d.setTitle(link.getTitle());
        d.setOriginalUrl(link.getOriginalUrl());
        d.setAlias(link.getAlias());
        d.setShortUrl("http://localhost:8000/" + link.getAlias());
        d.setClicks(link.getClicks());
        d.setWeekClicks(link.getWeekClicks());
        d.setPasswordProtected(link.isPasswordProtect());
        d.setExpiresAt(link.getExpiresAt());
        d.setEnableAnalytics(link.isEnableAnalytics());
        return d;
    }

    @Transactional
    public void registerClick(Link link) {
        link.setClicks(link.getClicks() + 1);

        List<Integer> week = new ArrayList<>(link.getWeekClicks());
        int last = week.size() - 1;
        week.set(last, week.get(last) + 1);
        link.setWeekClicks(week);

        linkRepo.save(link);
    }

    public long getRemainingLockSeconds(Link link) {
        if (link.getLockedUntil() == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (link.getLockedUntil().isBefore(now)) return 0;
        return Duration.between(now, link.getLockedUntil()).getSeconds();
    }

    @Transactional
    public String verifyPasswordAndCreateToken(String alias, String rawPassword, int ttlSeconds) {

        Link link = getLinkByAlias(alias);

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Link expired");
        }

        long lock = getRemainingLockSeconds(link);
        if (lock > 0) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Link locked for " + lock + " seconds");
        }

        if (link.getPasswordHash() == null) return null;

        boolean matches = passwordEncoder.matches(rawPassword, link.getPasswordHash());

        if (matches) {
            link.setFailedAttempts(0);
            link.setLockedUntil(null);
            linkRepo.save(link);

            String token = UUID.randomUUID().toString();
            tempTokens.put(token, new TempToken(alias,
                    LocalDateTime.now().plusSeconds(ttlSeconds)));

            return token;
        }

        int attempts = link.getFailedAttempts() + 1;
        link.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            link.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
        }

        linkRepo.save(link);
        return null;
    }

    @Transactional
    public boolean validateAndConsumeTempToken(String token, String alias) {
        TempToken t = tempTokens.get(token);
        if (t == null) return false;
        if (!t.alias.equals(alias)) return false;
        if (t.expiresAt.isBefore(LocalDateTime.now())) {
            tempTokens.remove(token);
            return false;
        }

        tempTokens.remove(token);
        return true;
    }
}
