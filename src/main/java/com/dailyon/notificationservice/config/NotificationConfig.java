package com.dailyon.notificationservice.config;


import java.util.UUID;

public class NotificationConfig {
    public static final String NOTIFICATIONS_STREAM_KEY = "notifications:stream";
    public static final Long NOTIFICATION_STREAM_TTL = 5 * 60 * 1000L; // ms단위 5분

    public static final String MEMBER_NOTIFICATION_CONNECTION_CHANNEL = "memberNotificationConnection";

    public static final String CONSUMER_GROUP_NAME = "notification-group";
    public static final String UNIQUE_CONSUMER_IDENTIFIER = CONSUMER_GROUP_NAME + "-" + UUID.randomUUID();
    public static final String AUCTION_REDIS_KEY = "auction";
}
