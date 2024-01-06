package com.dailyon.notificationservice.domain.notification.repository;


import com.dailyon.notificationservice.domain.notification.document.RestockNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public interface RestockNotificationRepository extends ReactiveMongoRepository<RestockNotification, String> {
    Mono<RestockNotification> findByProductIdAndSizeId(String productId, String sizeId);
}
