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
        String baseUrl = "https://dailyon.lotteedu.com";

        switch (notificationType) {
            case PRODUCT_RESTOCK:
                return baseUrl + "/products/" + parameters.getOrDefault("productId", "");
            case ORDER_COMPLETE:
            case ORDER_SHIPPED:
            case ORDER_ARRIVED:
            case ORDER_CANCELED:
                return baseUrl + "/order-history";
            case GIFT_RECEIVED:
                return baseUrl + "/gifts";
            case POINTS_EARNED_SNS:
                return baseUrl + "/point-history";
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
        String totalAmount = parameters.getOrDefault("totalAmount", "");
        String productName = parameters.getOrDefault("productName", "");

        String cancelAmount = parameters.getOrDefault("cancelAmount", "");;
        String productQuantity = parameters.getOrDefault("productQuantity", "");;

        String sizeName = parameters.getOrDefault("sizeName", "");
        String productId = parameters.getOrDefault("productId", "");
        String point = parameters.getOrDefault("point", "");
        String nickname = parameters.getOrDefault("nickname", "");

        switch (notificationType) {
            case PRODUCT_RESTOCK:
                return String.format("%s 상품의 %s 사이즈가 재입고되었습니다. 지금 확인해보세요!", productName, sizeName);
            case ORDER_COMPLETE:
                String orderCompletePostfix = !totalAmount.isEmpty() ? "주문금액: " + totalAmount: "";
                return String.format("주문번호: %s 의 주문이 완료되었습니다. %s", orderId, orderCompletePostfix);
            case ORDER_SHIPPED:
                return String.format("주문번호: %s 의 배송이 시작되었습니다.", orderId);
            case ORDER_ARRIVED:
                return String.format("주문번호: %s 의 배송이 완료되었습니다.", orderId);
            case ORDER_CANCELED:
                return String.format("주문이 취소되었습니다. \n 환불금액: %s, 상품명: %s, 개수: %s", cancelAmount, productName, productQuantity);
            case GIFT_RECEIVED:
                String giftPrefix = !nickname.isEmpty() ? nickname + "님을 위한 " : "";
                return String.format("%s선물이 도착했습니다. 선물함을 확인해주세요.", giftPrefix);
            case POINTS_EARNED_SNS:
                return String.format("OOTD 게시글을 통한 상품 판매로 포인트가 적립되었습니다: %s P", point);
            case AUCTION_END:
                return "참여하신 경매가 종료되었습니다. 결과를 확인해주세요.";
            default:
                return "";
        }

    }
}