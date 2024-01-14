package com.dailyon.notificationservice.config;


public class NotificationConfig {
    public static final String NOTIFICATIONS_STREAM_KEY = "notifications:stream";
    public static final Long NOTIFICATION_STREAM_TTL = 5 * 60 * 1000L; // ms단위 5분
}
