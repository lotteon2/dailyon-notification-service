package com.dailyon.notificationservice.domain.notification.api;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.service.NotificationService;
import com.dailyon.notificationservice.domain.notification.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    

}