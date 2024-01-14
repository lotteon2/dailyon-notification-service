package com.dailyon.notificationservice.domain.notification.repository;


import com.dailyon.notificationservice.domain.notification.document.UserNotification;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserNotificationRepository extends ReactiveMongoRepository<UserNotification, String> {

    Mono<UserNotification> findByMemberId(Long memberId);

}
