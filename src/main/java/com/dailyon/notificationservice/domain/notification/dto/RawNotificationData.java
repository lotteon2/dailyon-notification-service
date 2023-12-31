package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class RawNotificationData {
    private String message;

    // notification type과 parameters를 조합하여 linkUrl을 생성
    // notification type에 따라 parameters 조합하여 message 재가공 (type에 따라 할 수도 있고 안할수도 있음)
    private Map<String, String> parameters;
    private NotificationType notificationType; // 알림 유형
}
