package com.dailyon.notificationservice.domain.notification.repository;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserNotificationRepository extends ReactiveMongoRepository<UserNotification, String> {

    Flux<UserNotification> findTop5ByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Sort sort);
    Flux<UserNotification> findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Sort sort);
    Mono<Long> countByMemberIdAndUnreadNotInAndDeletedFalse(Long memberId, List<String> emptyList);

    //
//    Mono<Void> setNotificationRead(String notificationId, Long memberId);
//    Mono<Void> setAllNotificationsReadForMember(Long memberId);
//    Mono<Void> setNotificationDeleted(String notificationId, Long memberId);
//    Mono<Void> setAllNotificationsDeletedForMember(Long memberId);
}
