package com.dailyon.notificationservice.domain.notification.service;


import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.dailyon.notificationservice.domain.notification.repository.RestockNotificationRepository;
import com.dailyon.notificationservice.domain.notification.repository.UserNotificationRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationService {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final RestockNotificationRepository restockNotificationRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    private final Map<Long, Sinks.Many<ServerSentEvent<NotificationData>>> userSinks = new ConcurrentHashMap<>();

    // 구독하기. 구독 SSE 객체에는 client에게 줄 notificationData를 넣어준다.
    public Flux<ServerSentEvent<NotificationData>> streamNotifications(Long memberId) {
        log.info("Creating new SSE Sink for memberId: {}", memberId);
        Sinks.Many<ServerSentEvent<NotificationData>> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.put(memberId, sink);

        // log.info(userSinks.toString());

        Consumer<Throwable> removeSinkConsumer = e -> {
            userSinks.remove(memberId);
            log.info("Remove SSE Sink for memberId: {} due to error", memberId, e);
        };

        return sink.asFlux()
                .doOnCancel(() -> removeSinkConsumer.accept(null))
                .doOnError(removeSinkConsumer);
    }

    private Mono<Void> sendSseNotificationToUser(NotificationData data, Long memberId) {
        log.info("단일 유저에게 발송합니다.: {}", memberId);
        return Mono.fromRunnable(() -> {
            Optional.ofNullable(userSinks.get(memberId)).ifPresent(sink -> {
                log.info("memberId: {}를 찾았습니다. 이제 메세지 발송합니다.", memberId);
                sink.tryEmitNext(ServerSentEvent.<NotificationData>builder()
                                .data(data)
                                .build())
                        .orThrow();
            });
        });
    }

    public Mono<Void> sendNotificationToConnectedUsers(List<Long> memberIds, NotificationData notificationData) {
        log.info("연결된 유저들에게 발송합니다.: {}", memberIds.toString());
        return Flux.fromIterable(memberIds)
                .flatMap(memberId -> sendSseNotificationToUser(notificationData, memberId))
                .then();
    }

    private Mono<Void> sendSseNotificationToAllUsers(NotificationData data) {
        log.info("모든 유저에게 메세지 발송할것입니다.");
        return Flux.fromIterable(new ArrayList<>(userSinks.keySet()))
                .flatMap(memberId -> sendSseNotificationToUser(data, memberId)).then();
    }

    public Mono<Void> clearProductRestockNotifications(Long productId, Long sizeId) {
        return restockNotificationRepository.findByProductIdAndSizeId(productId, sizeId)
                .flatMap(restockNotification -> {
                    restockNotification.getMemberIds().clear();
                    return restockNotificationRepository.save(restockNotification);
                }).then();
    }

    public boolean isUserConnected(Long memberId) {
        return userSinks.containsKey(memberId);
    }
}
