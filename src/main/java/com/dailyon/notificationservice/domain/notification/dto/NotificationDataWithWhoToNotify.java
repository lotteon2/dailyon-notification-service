package com.dailyon.notificationservice.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDataWithWhoToNotify {
    List<Long> whoToNotify;
    NotificationData notificationData;

    public static NotificationDataWithWhoToNotify create(List<Long> whoToNotify, NotificationData notificationData) {
        return NotificationDataWithWhoToNotify.builder()
                .whoToNotify(whoToNotify)
                .notificationData(notificationData)
                .build();
    }
}
