package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.config.NotificationConfig;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.dto.NotificationDataWithWhoToNotify;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.dailyon.notificationservice.config.NotificationConfig.UNIQUE_CONSUMER_IDENTIFIER;

@Slf4j
@Service
public class RedisStreamNotificationService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;

    /*
    init뒤 구독정보 초기화 로직
    initializeConsumerGroup를 해보고, 결과와 관계없이(then) getLastStreamEntryId 통해 streams의 lastRecordId 구함.
    이후, lastEntryId부터 구독 시작. (서버 인스턴스 실행시 이전에 쌓인 데이터는 무시하고 현재 시각부터 들어오는 데이터에 대한 SSE 공유를 위함)
     */
    public RedisStreamNotificationService(
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper,
            SseNotificationService sseNotificationService) {
        this.reactiveRedisTemplate = Objects.requireNonNull(reactiveRedisTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.sseNotificationService = Objects.requireNonNull(sseNotificationService);

        Mono.just(NotificationConfig.NOTIFICATIONS_STREAM_KEY)
                .flatMap(this::initializeConsumerGroup)
                .flatMap(success -> getLastStreamEntryId(NotificationConfig.NOTIFICATIONS_STREAM_KEY))
                .flatMap(lastEntryId -> consumeNotificationsFrom(UNIQUE_CONSUMER_IDENTIFIER, NotificationConfig.NOTIFICATIONS_STREAM_KEY))
                .subscribe(result -> log.info("redis streams 구독 시작합니다."),
                        error -> log.error("redis streams 구독에 실패했습니다...", error));
    }


    private Mono<Boolean> initializeConsumerGroup(String streamKey) {
        log.info("initializeConsumerGroup 진입");
        return reactiveRedisTemplate.opsForStream()
                .createGroup(streamKey, ReadOffset.latest(), UNIQUE_CONSUMER_IDENTIFIER)
                .thenReturn(true) // 성공 시 true 반환
                .onErrorResume(e -> {
                    return Mono.just(false);
                });
    }

    private Mono<RecordId> getLastStreamEntryId(String streamKey) {
        log.info("getLastStreamEntryId 진입");
        return reactiveRedisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .reduce((first, second) -> second) // 스트림의 마지막 요소를 추출함.
                .map(MapRecord::getId) // 마지막 MapRecord의 ID를 가져옴.
                .switchIfEmpty(Mono.just(RecordId.autoGenerate())); // 스트림이 비어있을 경우 자동 생성된 ID를 사용.
    }

    private Mono<Void> consumeNotificationsFrom(String groupName, String streamKey) {
        log.info("consumeNotificationsFrom 진입");
        String consumerName = UUID.randomUUID().toString();
        StreamReadOptions readOptions = StreamReadOptions.empty().count(50);
        // Use the '>' ID to read new messages for the group
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        Flux<MapRecord<String, Object, Object>> messageFlux = reactiveRedisTemplate
                .opsForStream()
                .read(Consumer.from(groupName, consumerName), readOptions, streamOffset);

        // 메시지 처리 로직과 반복, 딜레이 적용
        return messageFlux
                .doOnNext(this::processRecord)
                .doOnError(this::processError)
                .repeat() // 스트림이 완료되면 재시작.
                .then();
//                .delayElements(Duration.ofSeconds(1)) // 과한 폴링 방지
//                .subscribe();

    }

    /*
     * 10분마다 redis stream 크기 정리하는 로직
     */
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

    public Mono<Long> trimStream(String key, long maxLength) {
        return reactiveRedisTemplate.opsForStream()
                .trim(key, maxLength);
        // spring boot 버전이 낮아서 .trim(key, StreamTrimOptions.trim().maxLen(maxLength)).next(); 못 씀.
    }

    /*
     * stream으로 들어온 데이터 처리로직
     */
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
                .acknowledge(NotificationConfig.NOTIFICATIONS_STREAM_KEY, UNIQUE_CONSUMER_IDENTIFIER, record.getId().getValue())
                .subscribe();
    }

    private void processError(Throwable err) {
        log.error("Redis Stream 처리 중 에러: ", err);
    }

}