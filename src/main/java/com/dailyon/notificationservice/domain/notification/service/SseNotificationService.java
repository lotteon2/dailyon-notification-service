package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class SseNotificationService {
    private final NotificationTemplateRepository notificationRepository;
    private final ConcurrentHashMap<Long, Sinks.Many<ServerSentEvent<NotificationTemplate>>> userSinks
            = new ConcurrentHashMap<>();

    // 구독하기
    public Flux<ServerSentEvent<NotificationTemplate>> streamNotifications(Long memberId) {
        Sinks.Many<ServerSentEvent<NotificationTemplate>> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.put(memberId, sink);

        Consumer<Throwable> removeSinkConsumer = e -> userSinks.remove(memberId);

        return sink.asFlux()
                .doOnCancel(() -> removeSinkConsumer.accept(null))
                .doOnError(removeSinkConsumer);
    }

    // TODO: AWS SQS 받으면 작성할 로직
    public void pushNotificationToMember(NotificationTemplate notification, Long memberId) {
        Sinks.Many<ServerSentEvent<NotificationTemplate>> sink = userSinks.get(memberId);
        if (sink != null) {
            sink.tryEmitNext(ServerSentEvent.builder(notification)
                    .event("notification-event")
                    .id(String.valueOf(notification.getId()))
                    .build())
                    .orThrow();
        }
    }

    // Kafka listener 대신 SQS를 쓰기로 결정했음.
    // TODO: AWS SQS 받으면 작성할 로직
    public Mono<Void> onNotificationReceived(NotificationTemplate notification, Long memberId) {
        return Mono.fromRunnable(() -> pushNotificationToMember(notification, memberId))
                .then(Mono.empty());
    }
}
