package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.dailyon.notificationservice.domain.notification.repository.UserNotificationRepository;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ReactiveMongoOperations mongoOperations;

    // 최근 5개 알림 받기
    public Flux<NotificationTemplate> getRecentNotifications(Long memberId) {
        // 여기서는 유저의 최근 알림 ID를 조회하고 NotificationTemplate에서 ID에 해당하는 내용을 가져옵니다.
        return userNotificationRepository.findTop5ByMemberIdAndDeletedFalseOrderByCreatedAtDesc(memberId, Sort.by(Sort.Direction.DESC, "createdAt"))
                .flatMap(userNotification -> notificationTemplateRepository.findById(userNotification.getId()));
    }

    // 모든 알림 조회
    public Flux<NotificationTemplate> getAllNotifications(Long memberId) {
        // 여기서는 유저의 모든 알림 ID를 조회하고 NotificationTemplate에서 ID에 해당하는 내용을 가져옵니다.
        return userNotificationRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(memberId, Sort.by(Sort.Direction.DESC, "createdAt"))
                .flatMap(userNotification -> notificationTemplateRepository.findById(userNotification.getId()));
    }

    // 안읽은 알림 개수 받기
    public Mono<Long> countUnreadNotifications(Long memberId) {
        // 여기서는 UserNotification에서 unread 상태인 알림의 수를 계산합니다.
        return userNotificationRepository.countByMemberIdAndUnreadNotInAndDeletedFalse(memberId, new ArrayList<>());
    }

    // 모든 알림 읽음 처리
    @Transactional
    public Mono<Void> markAllNotificationsAsRead(Long memberId) {
        Query query = Query.query(Criteria.where("memberId").is(memberId));
        Update update = new Update().set("read", true);

        return mongoOperations.updateMulti(query, update, UserNotification.class)
                .then();
    }

    // 개별 알림 읽음처리
    public Mono<Void> markNotificationAsRead(String notificationId, Long memberId) {
        Query query = new Query(Criteria.where("memberId").is(memberId).and("unread").is(notificationId));
        Update update = new Update().pull("unread", notificationId).addToSet("read", notificationId);

        return mongoOperations.findAndModify(query, update, UserNotification.class)
                .then();
    }

    // 모든 알림 삭제
    public Mono<Void> deleteAllNotifications(Long memberId) {
        Query query = Query.query(Criteria.where("memberId").is(memberId).and("deleted").is(false));
        Update update = new Update().set("deleted", true);

        return mongoOperations.updateMulti(query, update, UserNotification.class)
                .then();
    }

    // 개별 알림 삭제
    public Mono<Void> deleteNotification(String notificationId, Long memberId) {
        Query query = new Query(Criteria.where("memberId").is(memberId).andOperator(
                Criteria.where("unread").is(notificationId).orOperator(Criteria.where("read").is(notificationId))));
        Update update = new Update().pullAll("unread", new String[]{notificationId})
                .pullAll("read", new String[]{notificationId})
                .addToSet("deleted", notificationId);

        return mongoOperations.findAndModify(query, update, UserNotification.class)
                .then();
    }
}
