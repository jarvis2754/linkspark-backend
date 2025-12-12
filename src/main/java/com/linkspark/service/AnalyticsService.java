package com.linkspark.service;

import com.linkspark.dto.AnalyticsResponse;
import com.linkspark.model.Analytics;
import com.linkspark.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository repo;

    public void recordHit(String alias, HttpServletRequest request) {

        Analytics a = new Analytics();
        a.setAlias(alias);
        a.setIp(request.getRemoteAddr());
        a.setUserAgent(request.getHeader("User-Agent"));
        a.setReferer(request.getHeader("Referer"));

        String country = request.getHeader("CF-IPCountry");
        if (country == null || country.isBlank()) {
            country = request.getHeader("X-Country");
        }
        a.setCountry(country != null ? country : "UN");

        String ua = a.getUserAgent() != null ? a.getUserAgent().toLowerCase() : "";
        String device = "desktop";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) device = "mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) device = "tablet";
        a.setDevice(device);

        String browser = "Other";
        if (ua.contains("chrome") && !ua.contains("chromium") && !ua.contains("edg")) browser = "Chrome";
        else if (ua.contains("firefox")) browser = "Firefox";
        else if (ua.contains("safari") && !ua.contains("chrome")) browser = "Safari";
        else if (ua.contains("edg")) browser = "Edge";
        else if (ua.contains("opr") || ua.contains("opera")) browser = "Opera";
        a.setBrowser(browser);

        repo.save(a);
    }

    public AnalyticsResponse getMetricsForAlias(String alias) {
        return getMetricsForAlias(alias, "30d", null, null);
    }

    public AnalyticsResponse getMetricsForAlias(String alias, String range, String start, String end) {

        List<Analytics> rows = repo.findByAliasOrderByClickedAtDesc(alias);

        AnalyticsResponse resp = new AnalyticsResponse();
        resp.metrics = new AnalyticsResponse.Metrics();

        if (range.equals("24h")) {
            resp.metrics.timeseries = buildHourlySeries(rows);
        } else if (range.equals("7d")) {
            resp.metrics.timeseries = buildDailySeries(rows, 7);
        } else if (range.equals("30d")) {
            resp.metrics.timeseries = buildDailySeries(rows, 30);
        } else if (start != null && end != null) {
            resp.metrics.timeseries = buildCustomDailySeries(rows,
                    LocalDate.parse(start),
                    LocalDate.parse(end)
            );
        } else {
            resp.metrics.timeseries = buildDailySeries(rows, 30);
        }

        Map<String, Long> byCountry = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCountry() == null ? "UN" : r.getCountry(),
                        Collectors.counting()
                ));

        resp.metrics.countries = byCountry.entrySet().stream()
                .map(e -> {
                    AnalyticsResponse.CountryMetric m = new AnalyticsResponse.CountryMetric();
                    m.country = e.getKey();
                    m.name = e.getKey();
                    m.clicks = e.getValue();
                    return m;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        Map<String, Long> byDevice = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDevice() == null ? "desktop" : r.getDevice(),
                        Collectors.counting()
                ));

        resp.metrics.devices = byDevice.entrySet().stream()
                .map(e -> {
                    AnalyticsResponse.DeviceMetric m = new AnalyticsResponse.DeviceMetric();
                    m.type = e.getKey();
                    m.clicks = e.getValue();
                    return m;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        Map<String, Long> byBrowser = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBrowser() == null ? "Other" : r.getBrowser(),
                        Collectors.counting()
                ));

        resp.metrics.browsers = byBrowser.entrySet().stream()
                .map(e -> {
                    AnalyticsResponse.BrowserMetric m = new AnalyticsResponse.BrowserMetric();
                    m.name = e.getKey();
                    m.clicks = e.getValue();
                    return m;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        Map<String, Long> byRef = rows.stream()
                .map(r -> r.getReferer() == null ? "direct" : r.getReferer())
                .map(rf -> {
                    try {
                        var u = new java.net.URI(rf);
                        String host = u.getHost();
                        return (host == null) ? rf : host.replaceFirst("^www\\.", "");
                    } catch (Exception ex) {
                        return rf;
                    }
                })
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

        resp.metrics.referrers = byRef.entrySet().stream()
                .map(e -> {
                    AnalyticsResponse.ReferrerMetric m = new AnalyticsResponse.ReferrerMetric();
                    m.domain = e.getKey();
                    m.clicks = e.getValue();
                    return m;
                })
                .sorted((a, b) -> Long.compare(b.clicks, a.clicks))
                .toList();

        resp.recentClicks = rows.stream()
                .limit(50)
                .map(r -> {
                    AnalyticsResponse.RecentClick rc = new AnalyticsResponse.RecentClick();
                    rc.timestamp = r.getClickedAt();
                    rc.country = r.getCountry();
                    rc.referrer = r.getReferer();
                    rc.device = r.getDevice();
                    rc.browser = r.getBrowser();
                    return rc;
                })
                .toList();

        return resp;
    }

    private List<AnalyticsResponse.TimeSeriesPoint> buildHourlySeries(List<Analytics> rows) {

        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Long> bucket = new LinkedHashMap<>();

        for (int i = 23; i >= 0; i--) {
            int hour = now.minusHours(i).getHour();
            bucket.put(hour, 0L);
        }

        for (Analytics a : rows) {
            if (a.getClickedAt().isAfter(now.minusHours(24))) {
                int hour = a.getClickedAt().getHour();
                bucket.put(hour, bucket.get(hour) + 1);
            }
        }

        List<AnalyticsResponse.TimeSeriesPoint> list = new ArrayList<>();
        for (var e : bucket.entrySet()) {
            AnalyticsResponse.TimeSeriesPoint p = new AnalyticsResponse.TimeSeriesPoint();
            p.timestamp = now.withHour(e.getKey()).withMinute(0).withSecond(0).toString();
            p.clicks = e.getValue();
            list.add(p);
        }

        return list;
    }

    private List<AnalyticsResponse.TimeSeriesPoint> buildDailySeries(List<Analytics> rows, int days) {

        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> bucket = new LinkedHashMap<>();

        for (int i = days - 1; i >= 0; i--) {
            bucket.put(today.minusDays(i), 0L);
        }

        for (Analytics a : rows) {
            LocalDate d = a.getClickedAt().toLocalDate();
            if (bucket.containsKey(d)) {
                bucket.put(d, bucket.get(d) + 1);
            }
        }

        List<AnalyticsResponse.TimeSeriesPoint> list = new ArrayList<>();
        for (var e : bucket.entrySet()) {
            AnalyticsResponse.TimeSeriesPoint p = new AnalyticsResponse.TimeSeriesPoint();
            p.date = e.getKey().toString();
            p.clicks = e.getValue();
            list.add(p);
        }

        return list;
    }

    private List<AnalyticsResponse.TimeSeriesPoint> buildCustomDailySeries(List<Analytics> rows, LocalDate start, LocalDate end) {

        Map<LocalDate, Long> bucket = new LinkedHashMap<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            bucket.put(d, 0L);
        }

        for (Analytics a : rows) {
            LocalDate d = a.getClickedAt().toLocalDate();
            if (bucket.containsKey(d)) {
                bucket.put(d, bucket.get(d) + 1);
            }
        }

        List<AnalyticsResponse.TimeSeriesPoint> list = new ArrayList<>();
        for (var e : bucket.entrySet()) {
            AnalyticsResponse.TimeSeriesPoint p = new AnalyticsResponse.TimeSeriesPoint();
            p.date = e.getKey().toString();
            p.clicks = e.getValue();
            list.add(p);
        }

        return list;
    }
}
