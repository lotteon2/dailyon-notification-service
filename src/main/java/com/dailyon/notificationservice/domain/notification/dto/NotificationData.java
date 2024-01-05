package com.dailyon.notificationservice.domain.notification.dto;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import com.dailyon.notificationservice.domain.notification.dto.RawNotificationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class NotificationData {
    private String id;
    private String message;
    private String linkUrl;
    private NotificationType notificationType;
    private Boolean read;

    public static NotificationData from(NotificationTemplate notificationTemplate, Boolean read) {
        return NotificationData.builder()
                    .id(notificationTemplate.getId())
                    .message(notificationTemplate.getMessage())
                    .linkUrl(notificationTemplate.getLinkUrl())
                    .notificationType(notificationTemplate.getNotificationType())
                    .read(read)
                    .build();

    }

    public static NotificationData fromRawData(RawNotificationData rawNotificationData) {
        // linkUrl, message를 생성/가공
        String linkUrl = generateLinkUrl(rawNotificationData.getNotificationType(), rawNotificationData.getParameters());
        String message = rawNotificationData.getMessage(); // 필요하면 추후 message 가공하는 스태틱 메소드로 관리
        return NotificationData.builder()
                .id(null) // SQS 단계에서는 아직 ID가 생성되지 않았으므로 null 할당
                .message(message)
                .linkUrl(linkUrl)
                .notificationType(rawNotificationData.getNotificationType())
                .read(false)
                .build();
    }

    private static String generateLinkUrl(NotificationType notificationType, Map<String, String> parameters) {
        // 주문 완료 linkUrl 예시
        if (notificationType == NotificationType.ORDER_COMPLETE) {
            // parameters Map에서 order id나 다른 필요한 정보 추출
            String orderId = parameters.getOrDefault("orderId", "defaultOrderId");
            return "https://dailyon/orders/" + orderId; // url 변경 필요
        }
        // 추가 사례 생길때 분기처리

        return ""; // 매칭안되면 빈문자열
    }
}