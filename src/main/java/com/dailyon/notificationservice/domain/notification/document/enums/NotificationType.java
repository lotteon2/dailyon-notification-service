package com.dailyon.notificationservice.domain.notification.document.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    PRODUCT_RESTOCK("재입고", "상품 재입고 알림."),
    ORDER_COMPLETE("주문완료", "주문이 완료되었습니다."),
    ORDER_SHIPPED("선적", "주문하신 상품이 출발했습니다."),
    ORDER_ARRIVED("배송 도착", "주문하신 상품이 도착했습니다."),
    ORDER_CANCELED("주문 취소", "주문이 취소되었습니다."),
    AUCTION_END("실시간 경매 종료", "실시간 경매가 종료되었습니다."),
    GIFT_RECEIVED("선물", "선물을 받았습니다."),
    POINTS_EARNED_SNS("SNS 구매유도 포인트 적립", "SNS를 통해 포인트가 적립되었습니다."),
    HEARTBEAT("하트비트", "연결 유지용 주기적 송신."),
    WELCOME("웰컴메세지", "최초 접속 메세지");
    // 정의하면서 넣을 예정

    private final String name;
    private final String description;

    NotificationType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
