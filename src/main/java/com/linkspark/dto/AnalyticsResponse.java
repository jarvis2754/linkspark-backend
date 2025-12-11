package com.linkspark.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AnalyticsResponse {
    public Metrics metrics;
    public List<RecentClick> recentClicks;

    public static class Metrics {
        public List<TimeSeriesPoint> timeseries;
        public List<CountryMetric> countries;
        public List<DeviceMetric> devices;
        public List<BrowserMetric> browsers;
        public List<ReferrerMetric> referrers;
    }

    public static class TimeSeriesPoint {
        public String timestamp;
        public String date;
        public long clicks;
    }


    public static class CountryMetric {
        public String country; // code
        public String name; // optional display name
        public long clicks;
    }

    public static class DeviceMetric {
        public String type; // "mobile" | "desktop" | "tablet"
        public long clicks;
    }

    public static class BrowserMetric {
        public String name;
        public long clicks;
    }

    public static class ReferrerMetric {
        public String domain;
        public long clicks;
    }

    public static class RecentClick {
        public LocalDateTime timestamp;
        public String country;
        public String referrer;
        public String device;
        public String browser;
    }
}

