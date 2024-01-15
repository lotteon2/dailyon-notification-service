package com.dailyon.notificationservice.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedNotificationData {
    private List<Long> whoToNotify;
    private NotificationData notificationData;
    private Map<String, String> parameters;

    public static ExtendedNotificationData of(List<Long> whoToNotify, NotificationData notificationData, Map<String, String> parameters) {
        return ExtendedNotificationData.builder()
                .whoToNotify(whoToNotify)
                .notificationData(notificationData)
                .parameters(parameters)
                .build();
    }
}

