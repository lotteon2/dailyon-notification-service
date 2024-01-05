package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.dailyon.notificationservice.domain.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.mongodb.core.ReactiveMongoOperations;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ReactiveMongoOperations mongoOperations;

    // 최근 5개 unread 알림 받기
    public Flux<NotificationData> getRecentNotifications(Long memberId) {
        return userNotificationRepository.findByMemberId(memberId)
                .flatMapMany(userNotification -> notificationTemplateRepository.findByIdIn(userNotification.getUnread()))
                .sort(Comparator.comparing(NotificationTemplate::getCreatedAt).reversed())
                .take(5)
                .map(template -> NotificationData.from(template, false));
    }

    // 모든 알림 조회 (unread 및 read)
    public Flux<NotificationData> getAllNotifications(Long memberId) {
        return userNotificationRepository.findByMemberId(memberId)
                .flatMapMany(userNotification -> {
                    Flux<NotificationData> unreadNotifications = notificationTemplateRepository.findByIdIn(userNotification.getUnread())
                            .map(template -> NotificationData.from(template, false));
                    Flux<NotificationData> readNotifications = notificationTemplateRepository.findByIdIn(userNotification.getRead())
                            .map(template -> NotificationData.from(template, true));

                    return Flux.concat(unreadNotifications, readNotifications); // Fluxes 단일로 합쳐서 return
                });
    }

    // 안읽은 알림 개수 받기
    public Mono<Integer> countUnreadNotifications(Long memberId) {
        // 여기서는 UserNotification에서 unread 상태인 알림의 수를 계산합니다.
        return userNotificationRepository.findByMemberId(memberId)
                .map(UserNotification::getUnread)
                .map(Set::size)
                .switchIfEmpty(Mono.just(0)); // 문서가 없거나 unread 집합이 없는 경우 0 반환
    }


    // 모든 알림 읽음 처리
    @Transactional
    public Mono<Void> markAllNotificationsAsRead(Long memberId) {
        return userNotificationRepository.findByMemberId(memberId)
                .flatMap(userNotification -> {
                    userNotification.markAsReadAllNotifications();
                    return userNotificationRepository.save(userNotification);
                }).then();
    }

    // 개별 알림 읽음처리
    @Transactional
    public Mono<Void> markNotificationAsRead(String notificationId, Long memberId) {
        return userNotificationRepository.findByMemberId(memberId)
                .flatMap(userNotification -> {
                    userNotification.markAsRead(notificationId);
                    return userNotificationRepository.save(userNotification);
                }).then();
    }

    // 모든 알림 삭제
    @Transactional
    public Mono<Void> deleteAllNotifications(Long memberId) {
        return userNotificationRepository.findByMemberId(memberId)
                .flatMap(userNotification -> {
                    userNotification.deleteAllNotifications();
                    return userNotificationRepository.save(userNotification);
                }).then();
    }

    // 개별 알림 삭제
    @Transactional
    public Mono<Void> deleteNotification(String notificationId, Long memberId) {
        return userNotificationRepository
                .findByMemberId(memberId)
                .flatMap(userNotification -> {
                    userNotification.deleteNotification(notificationId);
                    return userNotificationRepository.save(userNotification);
                }).then();
    }

    public Mono<Void> createInitialUserNotification(Long memberId) {
        UserNotification newUserNotification = UserNotification.builder()
                .memberId(memberId)
                .unread(new HashSet<>())
                .read(new HashSet<>())
                .deleted(new HashSet<>())
                .build();
        return userNotificationRepository.insert(newUserNotification).then();
    }
}
