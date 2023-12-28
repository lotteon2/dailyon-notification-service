package com.dailyon.notificationservice.domain.notification.dto;

import lombok.Data;

import java.util.List;

@Data
public class SQSNotificationDto {
    List<Long> whoToNotify;

    NotificationData notificationData;
}
