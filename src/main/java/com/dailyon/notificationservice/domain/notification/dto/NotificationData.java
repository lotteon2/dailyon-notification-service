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
        String message = generateMessage(rawNotificationData.getNotificationType(),
                rawNotificationData.getParameters(),
                rawNotificationData.getMessage());

        return NotificationData.builder()
                .id(null) // SQS 단계에서는 아직 ID가 생성되지 않았으므로 null 할당
                .message(message)
                .linkUrl(linkUrl)
                .notificationType(rawNotificationData.getNotificationType())
                .read(false)
                .build();
    }

    private static String generateLinkUrl(NotificationType notificationType, Map<String, String> parameters) {
        String baseUrl = "";

        switch (notificationType) {
            case PRODUCT_RESTOCK:
                return baseUrl + "/products/" + parameters.getOrDefault("productId", "");
            case ORDER_COMPLETE:
            case ORDER_SHIPPED:
            case ORDER_ARRIVED:
                return baseUrl + "/my-page/order-history";
            case GIFT_RECEIVED:
                return baseUrl + "/my-page/gifts";
            case POINTS_EARNED_SNS:
                return baseUrl + "/my-page/point-history";
            case AUCTION_END:
                return "";
            default:
                return ""; // 매칭 안되면 빈문자열로 처리. FE에서 빈문자열일때 href를 주지않음.
        }
    }

    private static String generateMessage(NotificationType notificationType, Map<String, String> parameters, String defaultMessage) {
        if (defaultMessage != null && !defaultMessage.isEmpty()) {
            // 알림을 생성하는 micro-service에서 메세지를 직접 주면 그대로 넣어서 보냄.
            return defaultMessage;
        }

        String orderId = parameters.getOrDefault("orderId", "");
        String productName = parameters.getOrDefault("productName", "");
        String productId = parameters.getOrDefault("productId", "");
        String point = parameters.getOrDefault("point", "");

        switch (notificationType) {
            case PRODUCT_RESTOCK:
                return String.format("상품이름: %s 가 재입고 되었습니다.", productName);
            case ORDER_COMPLETE:
                return String.format("주문번호: %s 의 주문이 완료되었습니다.", orderId);
            case ORDER_SHIPPED:
                return String.format("주문번호: %s 의 배송이 시작되었습니다.", orderId);
            case ORDER_ARRIVED:
                return String.format("주문번호: %s 의 배송이 완료되었습니다.", orderId);
            case GIFT_RECEIVED:
                return "선물이 도착했습니다. 선물함을 확인해주세요.";
            case POINTS_EARNED_SNS:
                return String.format("OOTD 게시글을 통한 상품 판매로 포인트가 적립되었습니다: %s P", point);
            case AUCTION_END:
                return "참여하신 경매가 종료되었습니다. 결과를 확인해주세요.";
            default:
                return "";
        }

    }
}