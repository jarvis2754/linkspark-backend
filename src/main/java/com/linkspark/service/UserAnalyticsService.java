package com.linkspark.service;

import com.linkspark.dto.UserAnalyticsResponse;
import com.linkspark.model.Analytics;
import com.linkspark.model.Link;
import com.linkspark.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAnalyticsService {

    private final AnalyticsRepository analyticsRepo;
    private final LinkService linkService;

    public UserAnalyticsResponse getUserMetrics(Authentication auth, String range, String start, String end) {

        UUID userId = linkService.getUserId(auth);

        List<Link> userLinks = linkService.getAllLinksOfUser(userId);

        List<String> aliases = userLinks.stream()
                .map(Link::getAlias)
                .toList();

        List<Analytics> rows = analyticsRepo.findAll().stream()
                .filter(a -> aliases.contains(a.getAlias()))
                .sorted(Comparator.comparing(Analytics::getClickedAt).reversed())
                .toList();

        UserAnalyticsResponse resp = new UserAnalyticsResponse();
        resp.metrics = new UserAnalyticsResponse.Metrics();

        if (range.equals("7d"))
            resp.metrics.timeseries = buildDaily(rows, 7);
        else if (range.equals("30d"))
            resp.metrics.timeseries = buildDaily(rows, 30);
        else if (range.equals("90d"))
            resp.metrics.timeseries = buildDaily(rows, 90);
        else if (start != null && end != null)
            resp.metrics.timeseries = buildCustomDaily(rows, LocalDate.parse(start), LocalDate.parse(end));
        else
            resp.metrics.timeseries = buildDaily(rows, 7);

        resp.metrics.totalClicks = rows.size();
        resp.metrics.totalLinks = userLinks.size();

        resp.metrics.countries = rows.stream()
                .collect(Collectors.groupingBy(a -> a.getCountry(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    var c = new UserAnalyticsResponse.CountryMetric();
                    c.country = e.getKey();
                    c.name = e.getKey();
                    c.clicks = e.getValue();
                    return c;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.metrics.devices = rows.stream()
                .collect(Collectors.groupingBy(a -> a.getDevice(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    var d = new UserAnalyticsResponse.DeviceMetric();
                    d.type = e.getKey();
                    d.clicks = e.getValue();
                    return d;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.metrics.browsers = rows.stream()
                .collect(Collectors.groupingBy(a -> a.getBrowser(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    var b = new UserAnalyticsResponse.BrowserMetric();
                    b.name = e.getKey();
                    b.clicks = e.getValue();
                    return b;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.metrics.referrers = rows.stream()
                .map(a -> a.getReferer() == null ? "direct" : a.getReferer())
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    var r = new UserAnalyticsResponse.ReferrerMetric();
                    r.domain = cleanDomain(e.getKey());
                    r.clicks = e.getValue();
                    return r;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.topLinks = userLinks.stream().map(l -> {

            List<Analytics> linkRows = rows.stream()
                    .filter(a -> a.getAlias().equals(l.getAlias()))
                    .toList();

            UserAnalyticsResponse.TopLink t = new UserAnalyticsResponse.TopLink();
            t.linkId = l.getId();
            t.url = l.getOriginalUrl();
            t.title = l.getTitle();
            t.clicks = linkRows.size();
            t.last24h = linkRows.stream()
                    .filter(a -> a.getClickedAt().isAfter(LocalDateTime.now().minusHours(24)))
                    .count();

            return t;

        }).sorted((a, b) -> Long.compare(b.clicks, a.clicks)).toList();

        return resp;
    }

    private String cleanDomain(String url) {
        try {
            URI u = new URI(url);
            return u.getHost() == null ? "direct" : u.getHost().replace("www.", "");
        } catch (Exception e) {
            return "direct";
        }
    }

    private List<UserAnalyticsResponse.TimeSeriesPoint> buildDaily(List<Analytics> rows, int days) {

        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> bucket = new LinkedHashMap<>();

        for (int i = days - 1; i >= 0; i--)
            bucket.put(today.minusDays(i), 0L);

        for (Analytics a : rows) {
            LocalDate d = a.getClickedAt().toLocalDate();
            if (bucket.containsKey(d))
                bucket.put(d, bucket.get(d) + 1);
        }

        return bucket.entrySet().stream().map(e -> {
            var p = new UserAnalyticsResponse.TimeSeriesPoint();
            p.date = e.getKey().toString();
            p.clicks = e.getValue();
            return p;
        }).toList();
    }

    private List<UserAnalyticsResponse.TimeSeriesPoint> buildCustomDaily(
            List<Analytics> rows, LocalDate start, LocalDate end) {

        Map<LocalDate, Long> bucket = new LinkedHashMap<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
            bucket.put(d, 0L);

        for (Analytics a : rows) {
            LocalDate d = a.getClickedAt().toLocalDate();
            if (bucket.containsKey(d))
                bucket.put(d, bucket.get(d) + 1);
        }

        return bucket.entrySet().stream().map(e -> {
            var p = new UserAnalyticsResponse.TimeSeriesPoint();
            p.date = e.getKey().toString();
            p.clicks = e.getValue();
            return p;
        }).toList();
    }
}
