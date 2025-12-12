package com.linkspark.dto;

import java.util.List;

public class UserAnalyticsResponse {

    public Metrics metrics;
    public List<TopLink> topLinks;

    public static class Metrics {
        public List<TimeSeriesPoint> timeseries;
        public List<CountryMetric> countries;
        public List<DeviceMetric> devices;
        public List<BrowserMetric> browsers;
        public List<ReferrerMetric> referrers;

        public long totalClicks;
        public long totalLinks;
    }

    public static class TimeSeriesPoint {
        public String date;
        public long clicks;
    }

    public static class CountryMetric {
        public String country;
        public String name;
        public long clicks;
    }

    public static class DeviceMetric {
        public String type;
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

    public static class TopLink {
        public Long linkId;
        public String url;
        public String title;
        public long clicks;
        public long last24h;
    }
}

