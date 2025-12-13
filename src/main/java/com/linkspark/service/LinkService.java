package com.linkspark.service;

import com.linkspark.domain.Team;
import com.linkspark.domain.TeamMember;
import com.linkspark.domain.User;
import com.linkspark.dto.CreateLinkRequest;
import com.linkspark.dto.LinkDto;
import com.linkspark.dto.TeamDto;
import com.linkspark.dto.UpdateLinkRequest;
import com.linkspark.model.Link;
import com.linkspark.repository.LinkRepository;
import com.linkspark.repository.TeamMemberRepository;
import com.linkspark.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;
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

    private TeamMember requireTeamMember(Team team, User user) {
        return memberRepo.findByTeamAndUser(team, user)
                .filter(m -> !m.isPending())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a team member"));
    }

    private void requireEditPermission(TeamMember m) {
        if (m.getRole().equals("viewer"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void requireDeletePermission(TeamMember m) {
        if (m.getRole().equals("viewer") || m.getRole().equals("editor"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @Transactional
    public String createLink(CreateLinkRequest req, Authentication auth) {

        User user = (User) auth.getPrincipal();

        String alias = (req.getCustomAlias() == null || req.getCustomAlias().isBlank())
                ? UUID.randomUUID().toString().substring(0, 6)
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
        link.setOwner(user);

        if (req.getTeamId() != null) {
            Team team = teamRepo.findById(req.getTeamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            TeamMember member = requireTeamMember(team, user);
            requireEditPermission(member);

            link.setTeam(team);
        }

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

    public List<LinkDto> getAllLinks(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return linkRepo.findAllAccessibleLinks(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LinkDto getOneLink(Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();

        Link link = linkRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (link.getTeam() == null) {
            if (!link.getOwner().getId().equals(user.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else {
            requireTeamMember(link.getTeam(), user);
        }

        return toDto(link);
    }

    @Transactional
    public LinkDto update(Long id, UpdateLinkRequest req, Authentication auth) {

        User user = (User) auth.getPrincipal();
        Link link = linkRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (link.getTeam() == null) {
            if (!link.getOwner().getId().equals(user.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else {
            TeamMember m = requireTeamMember(link.getTeam(), user);
            requireEditPermission(m);
        }

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

        return toDto(linkRepo.save(link));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {

        User user = (User) auth.getPrincipal();
        Link link = linkRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (link.getTeam() == null) {
            if (!link.getOwner().getId().equals(user.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else {
            TeamMember m = requireTeamMember(link.getTeam(), user);
            requireDeletePermission(m);
        }

        linkRepo.delete(link);
    }

    public boolean isAliasAvailable(String alias) {
        return !linkRepo.existsByAlias(alias);
    }

    public Link getLinkByAlias(String alias) {
        return linkRepo.findByAlias(alias)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
    }

    public long getRemainingLockSeconds(Link link) {
        if (link.getLockedUntil() == null) return 0;
        if (link.getLockedUntil().isBefore(LocalDateTime.now())) return 0;
        return Duration.between(LocalDateTime.now(), link.getLockedUntil()).getSeconds();
    }

    @Transactional
    public String verifyPasswordAndCreateToken(String alias, String rawPassword, int ttlSeconds) {

        Link link = getLinkByAlias(alias);

        if (link.getExpiresAt() != null &&
                link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Link expired");
        }

        long lock = getRemainingLockSeconds(link);
        if (lock > 0) {
            throw new ResponseStatusException(HttpStatus.LOCKED);
        }

        if (link.getPasswordHash() == null) return null;

        if (passwordEncoder.matches(rawPassword, link.getPasswordHash())) {
            link.setFailedAttempts(0);
            link.setLockedUntil(null);
            linkRepo.save(link);

            String token = UUID.randomUUID().toString();
            tempTokens.put(
                    token,
                    new TempToken(alias, LocalDateTime.now().plusSeconds(ttlSeconds))
            );
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

    @Transactional
    public void registerClick(Link link) {
        link.setClicks(link.getClicks() + 1);

        List<Integer> week = new ArrayList<>(link.getWeekClicks());
        int last = week.size() - 1;
        week.set(last, week.get(last) + 1);

        link.setWeekClicks(week);
        linkRepo.save(link);
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

        if (link.getTeam() != null) {
            d.setTeam(new TeamDto(
                    link.getTeam().getId(),
                    link.getTeam().getName()
            ));
        } else {
            d.setTeam(null);
        }

        return d;
    }


}
