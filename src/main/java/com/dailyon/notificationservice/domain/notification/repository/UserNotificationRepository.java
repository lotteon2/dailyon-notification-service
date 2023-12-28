package com.dailyon.notificationservice.domain.notification.repository;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface UserNotificationRepository extends ReactiveMongoRepository<UserNotification, String> {

    // memberId에 해당하는 UserNotification에서 최근 5개의 unread 알림 ID를 조회
    @Query(value = "{ 'memberId': ?0, 'deleted': false }",
            sort = "{ 'createdAt': -1 }",
            fields = "{ 'unread': 1 }")
    Flux<UserNotification> findRecentUnreadByMemberId(Long memberId, Pageable pageable);

    // memberId에 해당하는 UserNotification에서 모든 unread, read 알림 ID를 조회
    @Query(value = "{ 'memberId': ?0, 'deleted': false }",
            sort = "{ 'createdAt': -1 }",
            fields = "{ 'unread': 1, 'read': 1 }")
    Flux<UserNotification> findAllNotificationsByMemberId(Long memberId);

    Mono<UserNotification> findByMemberId(Long memberId);

}
