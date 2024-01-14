package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.config.NotificationConfig;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.NotificationDataWithWhoToNotify;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.ReadOffset;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class RedisStreamNotificationService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;

    public RedisStreamNotificationService(
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper,
            SseNotificationService sseNotificationService) {
        this.reactiveRedisTemplate = Objects.requireNonNull(reactiveRedisTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.sseNotificationService = Objects.requireNonNull(sseNotificationService);
        consumeNotifications(); // bean 등록하면서 구독 시작
    }

    private void consumeNotifications() {
        // <String, String, String> 쓰고 싶은데 Type이 안맞음. bean에는 String String String으로 해뒀음.
        Flux<MapRecord<String, Object, Object>> messageFlux = reactiveRedisTemplate
                .opsForStream()
                .read(StreamOffset.create(NotificationConfig.NOTIFICATIONS_STREAM_KEY, ReadOffset.lastConsumed()));

        messageFlux.subscribe(record -> {
                    try {
                        // <String, String, String> 쓰고 싶은데 Type이 안맞음. - 임시로 casting
                        String jsonNotification = (String) record.getValue().get("notification");
                        if (jsonNotification != null) {
                            NotificationDataWithWhoToNotify redisNotificationDto = objectMapper.readValue(jsonNotification, NotificationDataWithWhoToNotify.class);
                            List<Long> memberIds = redisNotificationDto.getWhoToNotify();
                            NotificationData notificationData = redisNotificationDto.getNotificationData();

                            sseNotificationService.sendNotificationToConnectedUsers(memberIds, notificationData).subscribe();
                        }
                    } catch (JsonProcessingException e) {
                        log.error("notification JSON deserializing 중 에러: ", e);
                    }
                }, err -> {
                    log.error("Redis Stream 처리 중 에러: ", err);
                });
    }
}