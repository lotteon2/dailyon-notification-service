package com.dailyon.notificationservice.domain.notification.infra.message;

import com.dailyon.notificationservice.common.exceptions.ErrorResponseException;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.RawNotificationData;
import com.dailyon.notificationservice.domain.notification.dto.SQSNotificationDto;
import com.dailyon.notificationservice.domain.notification.service.NotificationService;
import com.dailyon.notificationservice.domain.notification.service.NotificationUtils;
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
    private final NotificationUtils notificationUtils;

    @SqsListener(
            value = "order-complete-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCompleteNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {

        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "product-restock-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeProductRestockNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .then( Mono.defer(() -> {
                        Long productId = Long.valueOf(rawNotificationData.getParameters().get("productId"));
                        Long sizeId = Long.valueOf(rawNotificationData.getParameters().get("sizeId"));
                        return sseNotificationService.clearProductRestockNotifications(productId, sizeId); // 저장, 발송처리 이후 지우는 로직 호출
                    }))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "order-shipped-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderShippedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "order-arrived-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderArrivedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }


    @SqsListener(
            value = "order-canceled_notification_queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCanceledNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "auction-end-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeAuctionEndNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "gift-received-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeGiftReceivedNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }

    @SqsListener(
            value = "points-earned-sns-notification-queue",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumePointsEarnedBySNSNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {
        try {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData); // rawNotificationData -> 데이터 가공

            List<Long> existingMemberIds = sqsNotificationDto.getWhoToNotify();
            Mono<List<Long>> memberIdsMono = notificationUtils.determineMemberIds( // 알림 수신대상 존재 여부에 따라 가공
                    rawNotificationData.getNotificationType(),
                    rawNotificationData.getParameters(),
                    existingMemberIds);

            memberIdsMono
                    .flatMap(memberIds -> sseNotificationService.onNotificationReceived(notificationData, memberIds))
                    .subscribe(
                            null, // onNext: not needed here
                            error -> log.error("Error processing SQS message: {}", error.getMessage(), error),
                            ack::acknowledge
                    );
        } catch (JsonProcessingException | ErrorResponseException processingException) {
            log.error("Failed to parse SQS message: {}", message, processingException);
            ack.acknowledge();
        } catch (Exception e) {
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
                    .doOnError(error -> log.error("메세지 처리 도중 에러 발생 message: {}", error.getMessage(), error))
                    .doOnSuccess(o -> ack.acknowledge())
                    .subscribe();
        } catch (JsonProcessingException e) {
            log.error("deserializing 중 에러 발생: {}", e.getMessage(), e);
        }

    }

}
