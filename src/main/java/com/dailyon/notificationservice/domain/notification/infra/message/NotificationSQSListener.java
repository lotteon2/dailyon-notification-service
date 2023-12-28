package com.dailyon.notificationservice.domain.notification.infra.message;

import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.SQSNotificationDto;
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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSQSListener {
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;

    @SqsListener(
            value = "${cloud.aws.sqs.order-complete-notification-queue.name}",
            deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void consumeOrderCompleteNotificationCheckQueue(
            @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
            throws JsonProcessingException {

        try {
            // objectMapper 이용, 들어오는 message를 DTO를 변환 -> 이후 NotificationTemplate 객체 build.
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            NotificationData notificationData = sqsNotificationDto.getNotificationData();

            // memberId 단일/복수일 수 있음. onNotificationReceived에서 관리해줌.
            List<Long> memberIds = sqsNotificationDto.getWhoToNotify();
            sseNotificationService.onNotificationReceived(notificationData, memberIds)
                    .subscribe(
                            null, // onNext: not needed here
                            error -> {
                                // Log the error without acknowledging the message so it becomes visible again after visibility timeout
                                log.error("Error processing SQS message: {}", error.getMessage(), error);
                            },
                            () -> {
                                // SQS processing 성공
                                ack.acknowledge();
                                log.info("SQS message successfully acknowledged.");
                            }
                    );
        } catch (JsonProcessingException e) {
            // Log the exception without acknowledging the message so it becomes visible again after visibility timeout
            log.error("Failed to parse SQS message: {}", message, e);
        }
    }
    // 해당 topic으로 들어오는 message는 같은 이벤트이므로,
    // message는 List<Long> memberids = List<Long>,

    // NotificationTemplate를 mongoDB에 저장(NotificationTemplate에 내용 저장, UserNotification의 unread set에 NotificationTemplate의 새로 저장된 id값 추가)

    // NotificationData의 from(NotificationTemplate) 정적 스태틱 메소드를 이용해서 NotificationTemplate을 NotificationData로 변환해서
    // 대상 memberId의 userSinks에 넣기. (해당 NotificationData는 SSE로 즉시 발송됨. - 이 부분은 맞는지 검토필요.

}
