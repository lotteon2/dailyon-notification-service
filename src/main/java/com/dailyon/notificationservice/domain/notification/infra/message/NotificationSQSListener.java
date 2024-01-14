package com.dailyon.notificationservice.domain.notification.infra.message;

import com.dailyon.notificationservice.domain.notification.service.NotificationProcessingService;
import com.dailyon.notificationservice.domain.notification.service.NotificationService;
import com.dailyon.notificationservice.domain.notification.service.NotificationUtils;
import com.dailyon.notificationservice.domain.notification.service.SseNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSQSListener {
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;
    private final NotificationService notificationService;
    private final NotificationProcessingService notificationProcessingService;
    private final NotificationUtils notificationUtils;
    private final ReactiveStringRedisTemplate stringRedisTemplate;


    @SqsListener(
            value = "product-restock-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeProductRestockNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("product-restock-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "order-complete-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCompleteNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
            log.info("주문 완료 메세지 도착");
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("order-complete-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "order-shipped-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderShippedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("order-shipped-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "order-arrived-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderArrivedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("order-arrived-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }


    @SqsListener(
            value = "order-canceled-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCanceledNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("order-canceled-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "auction-end-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeAuctionEndNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("auction-end-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "gift-received-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeGiftReceivedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("gift-received-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "points-earned-sns-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumePointsEarnedBySNSNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        Mono.just(message)
                .flatMap(notificationProcessingService::processMessage)
                .then()
                .doOnError(error -> {
                    log.error("points-earned-sns-notification-queue 처리 중 에러: {}", error.getMessage(), error);
                    ack.acknowledge(); // Acknowledge in case of error
                })
                .doOnSuccess(aVoid -> ack.acknowledge()) // Acknowledge on success
                .subscribe();
    }

    @SqsListener(
            value = "user-created-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeUserCreatedCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
            throws JsonProcessingException {
        try {
            Long memberId = objectMapper.readValue(message, Long.class);
            notificationService.createInitialUserNotification(memberId)
                    .doOnError(error -> log.error("메세지 처리 도중 에러 발생 message: {}", error.getMessage(), error))
                    .doOnSuccess(o -> ack.acknowledge())
                    .subscribe();
        } catch (JsonProcessingException e) {
            log.error("deserializing 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}
