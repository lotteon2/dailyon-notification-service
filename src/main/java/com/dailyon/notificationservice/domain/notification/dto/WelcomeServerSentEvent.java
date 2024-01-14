package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import org.springframework.http.codec.ServerSentEvent;

public class WelcomeServerSentEvent {

    private static final NotificationData WELCOME_INSTANCE = NotificationData.builder()
            .id(null)
            .message(null)
            .linkUrl(null)
            .notificationType(NotificationType.WELCOME)
            .read(false)
            .build();

    private static final ServerSentEvent<NotificationData> WELCOME_EVENT = ServerSentEvent.<NotificationData>builder()
            .data(WELCOME_INSTANCE)
            .build();

    public static ServerSentEvent<NotificationData> getInstance() {
        return WELCOME_EVENT;
    }
}
