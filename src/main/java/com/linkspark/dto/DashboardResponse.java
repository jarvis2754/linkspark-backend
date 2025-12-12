package com.linkspark.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DashboardResponse {

    public Summary summary;
    public List<TopLink> topLinks;
    public List<Activity> activity;
    public List<Device> devices;
    public List<Browser> browsers;

    public static class Summary {
        public long totalClicks;
        public long activeLinks;
        public List<Long> weeklyClicks;
    }

    public static class TopLink {
        public Long id;
        public String title;
        public String originalUrl;
        public String shortUrl;
        public long clicks;
        public LocalDateTime expiresAt;
        public boolean passwordProtected;
    }

    public static class Activity {
        public String id;
        public String action;
        public String detail;
        public String time;
    }

    public static class Device {
        public String type;
        public long clicks;

        public Device(String type, long clicks) {
            this.type = type;
            this.clicks = clicks;
        }
    }

    public static class Browser {
        public String name;
        public long clicks;

        public Browser(String name, long clicks) {
            this.name = name;
            this.clicks = clicks;
        }
    }
}

