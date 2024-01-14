package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.config.NotificationConfig;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.NotificationDataWithWhoToNotify;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.lettuce.core.RedisBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class RedisStreamNotificationService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;

    private static final String CONSUMER_GROUP_NAME = "notification-group";

    public RedisStreamNotificationService(
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper,
            SseNotificationService sseNotificationService) {
        this.reactiveRedisTemplate = Objects.requireNonNull(reactiveRedisTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.sseNotificationService = Objects.requireNonNull(sseNotificationService);

        Mono.just(NotificationConfig.NOTIFICATIONS_STREAM_KEY)
                .flatMap(this::initializeConsumerGroup)
                .subscribe(result -> log.info("Consumer group initialized or already exists."),
                        error -> log.error("Error initializing consumer group", error));
        consumeNotifications(); // bean 등록하면서 구독 시작
    }
    private Mono<String> initializeConsumerGroup(String streamKey) {
        // Consumer Group 생성
        return reactiveRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), CONSUMER_GROUP_NAME)
                .doOnError(e -> log.info("Consumer Group 이미 존재, 새로 생성안함."))
                .onErrorResume(RedisBusyException.class, e -> Mono.empty()); // Ignore RedisBusyException
    }

    public Mono<Long> trimStream(String key, long maxLength) {
        return reactiveRedisTemplate.opsForStream()
                .trim(key, maxLength);
        // spring boot 버전이 낮아서 .trim(key, StreamTrimOptions.trim().maxLen(maxLength)).next(); 못 씀.
    }

    @Scheduled(fixedRate = 600000) // 10분마다
    public void scheduledStreamTrim() {
        log.info("redis 정리");
        trimStream(NotificationConfig.NOTIFICATIONS_STREAM_KEY, 1000) // 1000개
                .subscribe(result -> {
                    if (result != null) {
                        log.info("Stream trimmed, entries removed: {}", result);
                    }
                }, error -> {
                    log.error("Error occurred while trimming the stream: ", error);
                });
    }

    private void consumeNotifications() {
//        log.info("Redis 구독 시작");
        String consumerName = UUID.randomUUID().toString();
        StreamOffset<String> streamOffset = StreamOffset.create(NotificationConfig.NOTIFICATIONS_STREAM_KEY, ReadOffset.lastConsumed());
        StreamReadOptions readOptions = StreamReadOptions.empty().count(50);

        Flux<MapRecord<String, Object, Object>> messageFlux = reactiveRedisTemplate
                .opsForStream()
                .read(Consumer.from(CONSUMER_GROUP_NAME, consumerName), readOptions, streamOffset);

        messageFlux
                .doOnNext(this::processRecord)
                .doOnError(this::processError)
                .repeat() // 스트림이 완료되면 재시작.
                .delayElements(Duration.ofSeconds(1)) // 과한 polling 방지 1초간격
                .subscribe();
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        log.info("Redis 메시지 도착");
        try {
            String jsonNotification = (String) record.getValue().get("payload");
            if (jsonNotification != null) {
                processNotification(jsonNotification);
            }
        } catch (JsonProcessingException e) {
            log.error("Notification JSON deserializing 중 에러: ", e);
        }
        // ACK 보내기
        sendAcknowledgment(record);
    }

    private void processNotification(String jsonNotification) throws JsonProcessingException {
        NotificationDataWithWhoToNotify redisNotificationDto = objectMapper.readValue(jsonNotification, NotificationDataWithWhoToNotify.class);
        Long messageTimestamp = redisNotificationDto.getTimeStamp();
        Long currentTimestamp = System.currentTimeMillis();
//        log.info("processNotification 시작 {}, 현재 시각:{} DTO 생성 시각: {}", redisNotificationDto, currentTimestamp, messageTimestamp);
        if (currentTimestamp - messageTimestamp < NotificationConfig.NOTIFICATION_STREAM_TTL) {
            List<Long> memberIds = redisNotificationDto.getWhoToNotify();
            NotificationData notificationData = redisNotificationDto.getNotificationData();
            sseNotificationService.sendNotificationToConnectedUsers(memberIds, notificationData).subscribe();
        } else {
            log.info("설정 TTL 지나서 무시합니다. Timestamp: {}", messageTimestamp);
        }
    }

    private void sendAcknowledgment(MapRecord<String, Object, Object> record) {
        reactiveRedisTemplate.opsForStream()
                .acknowledge(NotificationConfig.NOTIFICATIONS_STREAM_KEY, CONSUMER_GROUP_NAME, record.getId().getValue())
                .subscribe();
    }

    private void processError(Throwable err) {
        log.error("Redis Stream 처리 중 에러: ", err);
    }


}