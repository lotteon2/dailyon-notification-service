package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class NotificationData {
    private String id;
    private String message;
    private String linkUrl;
    private NotificationType notificationType;

    public static NotificationData from(NotificationTemplate notificationTemplate) {
        return NotificationData.builder()
                    .id(notificationTemplate.getId())
                    .message(notificationTemplate.getMessage())
                    .linkUrl(notificationTemplate.getLinkUrl())
                    .notificationType(notificationTemplate.getNotificationType())
                    .build();

    }
}