package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class SseNotificationService {
    private final NotificationTemplateRepository notificationRepository;
    private final ConcurrentHashMap<Long, EmitterProcessor<ServerSentEvent<NotificationTemplate>>> userEmitters
            = new ConcurrentHashMap<>();

    // 구독하기
    public Flux<ServerSentEvent<NotificationTemplate>> streamNotifications(Long memberId) {
        EmitterProcessor<ServerSentEvent<NotificationTemplate>> emitter = EmitterProcessor.create();
        userEmitters.put(memberId, emitter);

        Consumer<Throwable> removeEmitterConsumer = e -> userEmitters.remove(memberId);

        return emitter
                .doOnCancel(() -> removeEmitterConsumer.accept(null))
                .doOnError(removeEmitterConsumer);
    }

    public void pushNotificationToMember(NotificationTemplate notification, Long memberId) {
        EmitterProcessor<ServerSentEvent<NotificationTemplate>> emitter = userEmitters.get(memberId);
        if (emitter != null && !emitter.isTerminated()) {
            emitter.onNext(ServerSentEvent.builder(notification)
                    .event("notification-event")
                    .id(String.valueOf(notification.getId()))
                    .build());
        }
    }

    // Imagine a method that is called by a Kafka listener just like in the original SseService.java
    // Here we illustrate how it might look by just using a placeholder
    // SQS 받으면 작성할 로직
    public Mono<Void> onNotificationReceived(NotificationTemplate notification, Long memberId) {
        return Mono.fromRunnable(() -> pushNotificationToMember(notification, memberId))
                .then(Mono.empty());
    }
    

}
