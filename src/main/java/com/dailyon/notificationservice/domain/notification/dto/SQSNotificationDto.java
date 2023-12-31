package com.dailyon.notificationservice.domain.notification.dto;

import lombok.Data;

import java.util.List;

@Data
public class SQSNotificationDto {
    List<Long> whoToNotify; // if null, 전체유저에게 발송

    RawNotificationData rawNotificationData;
}
