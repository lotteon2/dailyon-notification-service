package com.dailyon.notificationservice.domain.notification.infra.message;

import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.RawNotificationData;
import com.dailyon.notificationservice.domain.notification.dto.SQSNotificationDto;
import com.dailyon.notificationservice.domain.notification.service.NotificationService;
import com.dailyon.notificationservice.domain.notification.service.SseNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSQSListener {
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;
    private final NotificationService notificationService;

    @SqsListener(
            value = "order-complete-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCompleteNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
            throws JsonProcessingException {

        try {
            // objectMapper 이용, 들어오는 message를 DTO를 변환 -> 이후 NotificationTemplate 객체 build.
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();

            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 가공

            // memberIds가 null(현재 연결된 모든 유저)/단일/복수일 수 있음. onNotificationReceived에서 관리.
            List<Long> memberIds = sqsNotificationDto.getWhoToNotify();
            sseNotificationService.onNotificationReceived(notificationData, memberIds)
                    .subscribe(
                            null, // onNext: not needed here
                            error -> {
                                // visibility timeout 이후에 다시 처리하기 위해 ack 처리 안함.
                                log.error("Error processing SQS message: {}", error.getMessage(), error);
                            },
                            ack::acknowledge
                    );
        } catch (JsonProcessingException e) {
            // visibility timeout 이후에 다시 처리하기 위해 ack 처리 안함.
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }


    @SqsListener(
            value = "user-created-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeFirstLoginCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
            throws JsonProcessingException {
        try {
            Long memberId = objectMapper.readValue(message, Long.class);
            notificationService.createInitialUserNotification(memberId)
                    .subscribe(
                            null,
                            error -> log.error("Error processing message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message: {}", e.getMessage(), e);
        }

    }

}
