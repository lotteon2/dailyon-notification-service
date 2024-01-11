package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawNotificationData {
    /*
    message를 따로 넣어주면 해당 메세지로 발송,
    null 혹은 empty string으로 주면 NotificationType에 따라 parameters를 조합해 가공됨.
     */
    private String message;

    // NotificationType과 parameters를 조합하여 linkUrl을 생성
    private Map<String, String> parameters;
    private NotificationType notificationType; // 알림 유형
}
