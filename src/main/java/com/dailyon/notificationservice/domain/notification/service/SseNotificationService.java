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


    

}
