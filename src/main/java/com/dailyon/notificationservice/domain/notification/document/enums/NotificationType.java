package com.dailyon.notificationservice.domain.notification.document.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    PRODUCT_RESTOCK("재입고", "상품 재입고 알림."),
    ORDER_COMPLETE("주문완료", "주문이 완료되었습니다."),
    ORDER_SHIPPED("선적", "주문하신 상품이 출발했습니다.");
    // 정의하면서 넣을 예정

    private final String name;
    private final String description;

    NotificationType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
