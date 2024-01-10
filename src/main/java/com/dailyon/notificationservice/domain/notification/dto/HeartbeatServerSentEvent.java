package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import org.springframework.http.codec.ServerSentEvent;

public class HeartbeatServerSentEvent {

    private static final NotificationData HEARTBEAT_INSTANCE = NotificationData.builder()
            .id(null)
            .message(null)
            .linkUrl(null)
            .notificationType(NotificationType.HEARTBEAT)
            .read(false)
            .build();

    private static final ServerSentEvent<NotificationData> HEARTBEAT_EVENT = ServerSentEvent.<NotificationData>builder()
            .data(HEARTBEAT_INSTANCE)
            .build();

    public static ServerSentEvent<NotificationData> getInstance() {
        return HEARTBEAT_EVENT;
    }
}
