package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.common.exceptions.ErrorResponseException;
import com.dailyon.notificationservice.domain.notification.document.RestockNotification;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import com.dailyon.notificationservice.domain.notification.repository.RestockNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class NotificationUtils {
    private final SseNotificationService sseNotificationService;
    private final RestockNotificationRepository restockNotificationRepository;

    /**
     * existingMemberIds가 null or empty -> determineMemberIdsForNotification로 적절한 대상으로 대체, 아니라면 existingMemberIds 그대로 사용
     */
    public Mono<List<Long>> determineMemberIds(NotificationType notificationType, Map<String, String> parameters, List<Long> existingMemberIds) {
        return (existingMemberIds == null || existingMemberIds.isEmpty()) ?
                determineMemberIdsForNotification(notificationType, parameters) :
                Mono.just(existingMemberIds);
    }


    // 대상이 정해지지 않은 memberIds를 가공
    private Mono<List<Long>> determineMemberIdsForNotification(NotificationType notificationType, Map<String, String> parameters) {
        if (NotificationType.PRODUCT_RESTOCK.equals(notificationType)) {
            String productId = parameters.getOrDefault("productId", null);
            String sizeId = parameters.getOrDefault("sizeId", null);
            if (productId != null && sizeId != null) {
                return restockNotificationRepository.findByProductIdAndSizeId(productId, sizeId)
                        .map(RestockNotification::getMemberIds)
                        .defaultIfEmpty(Collections.emptyList()); // restockNotificationRepository 조회 후 보낼 대상이 없다면
            } else {
                throw new ErrorResponseException("productId 혹은 sizeId가 지정되지 않았습니다.");
            }
        }
        return Mono.just(Collections.emptyList());
    }
}
