package com.dailyon.notificationservice.domain.notification.api;

import com.dailyon.notificationservice.domain.notification.api.request.EnrollRestockRequest;
import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.dto.HeartbeatServerSentEvent;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.WelcomeServerSentEvent;
import com.dailyon.notificationservice.domain.notification.service.NotificationService;
import com.dailyon.notificationservice.domain.notification.service.RedisPubSubService;
import com.dailyon.notificationservice.domain.notification.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import com.amazonaws.services.sqs.model.SendMessageResult;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import javax.validation.Valid;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;
    private final RedisPubSubService redisPubSubService;
    // SQS 발행 테스트용 임시 코드
//    private final QueueMessagingTemplate queueMessagingTemplate;
//    private final String notificationQueue = "order-complete-notification-queue";
//    @PostMapping("/publish")
//    public void publishMessageToSqs(@RequestBody String notificationMessage) {
//        log.debug(notificationMessage);
//        Message<String> message = MessageBuilder.withPayload(notificationMessage).build();
//        queueMessagingTemplate.send(notificationQueue, message);
//    }


    // 최근 unread 알림 5개 받기 - 테스트완료
    @GetMapping("")
    public Flux<NotificationData> getRecentNotifications(@RequestHeader Long memberId) {
        return notificationService.getRecentNotifications(memberId);
    }

    // 모든 알림 조회 - 테스트완료
    @GetMapping("/all")
    public Flux<NotificationData> getAllNotifications(@RequestHeader Long memberId) {
        return notificationService.getAllNotifications(memberId);
    }

    // 안읽은 알림 개수 받기 - 테스트완료
    @GetMapping("/count")
    public Mono<Integer> getUnreadNotificationCount(@RequestHeader Long memberId) {
        return notificationService.countUnreadNotifications(memberId);
    }

    // 개별 알림 읽음처리 - 테스트완료
    @PutMapping("/{id}/read")
    public Mono<Void> markNotificationAsRead(
            @PathVariable String id, @RequestHeader Long memberId) {
        return notificationService.markNotificationAsRead(id, memberId);
    }

    // 모든 알림 읽음처리 - 테스트완료
    @PutMapping("/read/all")
    public Mono<Void> markAllNotificationsAsRead(@RequestHeader Long memberId) {
        return notificationService.markAllNotificationsAsRead(memberId);
    }

    // 개별 알림 삭제 - 테스트완료
    @DeleteMapping("/{id}")
    public Mono<Void> deleteNotification(
            @PathVariable String id, @RequestHeader Long memberId) {
        return notificationService.deleteNotification(id, memberId);
    }

    // 모든 알림 삭제 - 테스트완료
    @DeleteMapping("")
    public Mono<Void> deleteAllNotifications(@RequestHeader Long memberId) {
        return notificationService.deleteAllNotifications(memberId);
    }

    @PostMapping("/restock/enroll")
    public Mono<String> createOrUpdateRestockNotification(
            @Valid @RequestBody EnrollRestockRequest enrollRestockRequest,
            @RequestHeader Long memberId) {
        return notificationService.createOrUpdateRestockNotification(memberId, enrollRestockRequest);
    }

    // 구독하기 - 테스트완료 (SQS와 통합한 테스트 - 완료)
    @GetMapping(value = "/subscription", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationData>> subscribeToNotifications(@RequestHeader Long memberId) {
        log.info("SSE 연결 시작" + "memberId: " + memberId);

        Flux<ServerSentEvent<NotificationData>> welcomeFlux = Flux.just(WelcomeServerSentEvent.getInstance());
        
        Flux<ServerSentEvent<NotificationData>> notificationFlux = sseNotificationService.streamNotifications(memberId)
                .doOnCancel(() -> log.info("Notification stream 취소 - memberId: {}", memberId))
                .doFinally(signalType -> log.info("signal type {} 와 함께 Notification stream 종료 memberId: {}", signalType, memberId));

//        log.info("Notification stream Flux 생성됨 - memberId: {}", memberId);

        redisPubSubService.publishMemberConnection(memberId).subscribe();

        Flux<ServerSentEvent<NotificationData>> heartbeatFlux = Flux.interval(Duration.ofSeconds(15))
                .take(80) // 20분 송신 후 객체 정리 - 45초동안 연결없으면 client에서 자동으로 재연결
                .map(tick -> HeartbeatServerSentEvent.getInstance())
                .doOnNext(tick -> log.info("Heartbeat event sent - memberId: {}", memberId));

        return Flux.concat(welcomeFlux, Flux.merge(notificationFlux, heartbeatFlux))
                .doOnError(e -> log.error("Error in SSE stream for member {}: {}", memberId, e.getMessage(), e))
                .doOnTerminate(() -> log.info("SSE stream for member {} 종료", memberId));
    }

}