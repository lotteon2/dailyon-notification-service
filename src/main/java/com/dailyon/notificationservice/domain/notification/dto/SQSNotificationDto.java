package com.dailyon.notificationservice.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SQSNotificationDto {
    List<Long> whoToNotify; // if null, 전체유저 혹은 notificationType에 따라 지정된 대상에게 발송

    RawNotificationData rawNotificationData;
}
