package com.dailyon.notificationservice.domain.notification.repository;

import org.springframework.stereotype.Repository;
import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationTemplateRepository extends ReactiveMongoRepository<NotificationTemplate, String> {

        // 개별 알림 읽음 처리 쿼리
//        @Query("{'$and': [{'_id': ?0}, {'memberId': ?1}]}")
//        Mono<Void> setNotificationRead(String notificationId, Long memberId);

        // 모든 알림 읽음 처리 쿼리
//        @Query("{'memberId': ?0}")
//        Mono<Void> setAllNotificationsReadForMember(Long memberId);

        // 개별 알림 삭제 쿼리
//        @Query("{'$and': [{'_id': ?0}, {'memberId': ?1}]}")
//        Mono<Void> setNotificationDeleted(String notificationId, Long memberId);

        // 모든 알림 삭제 쿼리
//        @Query("{'memberId': ?0}")
//        Mono<Void> setAllNotificationsDeletedForMember(Long memberId);

}
