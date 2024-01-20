package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.common.exceptions.ErrorResponseException;
import com.dailyon.notificationservice.domain.notification.document.RestockNotification;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import com.dailyon.notificationservice.domain.notification.repository.RestockNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class NotificationUtils {
    private final SseNotificationService sseNotificationService;
    private final RestockNotificationRepository restockNotificationRepository;
    private final RedisUtilService redisUtilService;

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
            Long productId = Long.valueOf(parameters.getOrDefault("productId", null)); // 없으면 NumberFormatException
            Long sizeId = Long.valueOf(parameters.getOrDefault("sizeId", null));
            return restockNotificationRepository.findByProductIdAndSizeId(productId, sizeId)
                    .map(restockNotification -> new ArrayList<>(restockNotification.getMemberIds()));
        } else if (NotificationType.AUCTION_END.equals(notificationType)) {
            return redisUtilService.fetchAllAuctionMemberIds()
                    .collectList();
        }
        return Mono.just(Collections.emptyList());
    }
}
