package com.linkspark.service;

import com.linkspark.domain.User;
import com.linkspark.dto.DashboardResponse;
import com.linkspark.model.Analytics;
import com.linkspark.model.Link;
import com.linkspark.repository.AnalyticsRepository;
import com.linkspark.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LinkRepository linkRepo;
    private final AnalyticsRepository analyticsRepo;

    public DashboardResponse buildDashboard(Authentication auth) {

        User user = (User) auth.getPrincipal();
        UUID userId = user.getId();

        List<Link> links = linkRepo.findByOwner(user);
        List<String> aliases = links.stream().map(Link::getAlias).toList();

        List<Analytics> rows = analyticsRepo.findAll().stream()
                .filter(a -> aliases.contains(a.getAlias()))
                .sorted(Comparator.comparing(Analytics::getClickedAt).reversed())
                .toList();

        DashboardResponse resp = new DashboardResponse();

        resp.summary = new DashboardResponse.Summary();
        resp.summary.totalClicks = rows.size();
        resp.summary.activeLinks = links.size();

        // ---- weekly clicks (rolling last 7 days)
        LocalDate today = LocalDate.now();
        long[] weekly = new long[7];

        for (Analytics a : rows) {
            LocalDate d = a.getClickedAt().toLocalDate();
            long diff = java.time.temporal.ChronoUnit.DAYS.between(d, today);
            if (diff >= 0 && diff < 7) {
                int index = (int) (6 - diff);
                weekly[index]++;
            }
        }

        resp.summary.weeklyClicks = Arrays.stream(weekly).boxed().toList();

        // ---------------------------
        // TOP LINKS
        // ---------------------------
        resp.topLinks = links.stream()
                .map(l -> {
                    long clicks = rows.stream()
                            .filter(a -> a.getAlias().equals(l.getAlias()))
                            .count();

                    DashboardResponse.TopLink t = new DashboardResponse.TopLink();
                    t.id = l.getId();
                    t.title = l.getTitle();
                    t.originalUrl = l.getOriginalUrl();
                    t.shortUrl = "http://localhost:8000/" + l.getAlias();
                    t.clicks = clicks;
                    t.expiresAt = l.getExpiresAt();
                    t.passwordProtected = l.isPasswordProtect();

                    return t;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .limit(5)
                .toList();


        // ---------------------------
        // ACTIVITY FEED
        // ---------------------------
        resp.activity = rows.stream()
                .limit(10)
                .map(a -> {
                    DashboardResponse.Activity item = new DashboardResponse.Activity();
                    item.id = UUID.randomUUID().toString();
                    item.action = "Link Clicked";
                    item.detail = "Clicked from " + a.getCountry() + " (" + a.getBrowser() + ")";
                    item.time = formatTimeAgo(a.getClickedAt());
                    return item;
                })
                .toList();

        resp.devices = rows.stream()
                .collect(Collectors.groupingBy(Analytics::getDevice, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> new DashboardResponse.Device(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.browsers = rows.stream()
                .collect(Collectors.groupingBy(Analytics::getBrowser, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> new DashboardResponse.Browser(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        return resp;
    }

    private String formatTimeAgo(LocalDateTime time) {
        long mins = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}

