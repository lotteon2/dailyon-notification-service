package com.dailyon.notificationservice.domain.notification.api;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
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

    // 구독하기 - 테스트완료 (SQS와 통합한 테스트 필요)
    @GetMapping(value = "/subscription", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationData>> subscribeToNotifications(@RequestHeader Long memberId) {
        return sseNotificationService.streamNotifications(memberId);
    }

}