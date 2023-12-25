package com.dailyon.notificationservice.domain.notification.document.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    PRODUCT_RESTOCK("재입고", "상품 재입고 알림."),
    ORDER_SHIPPED("선적", "주문하신 상품이 출발했습니다.");


    private final String name;
    private final String description;

    NotificationType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
