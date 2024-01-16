package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.dto.DisconnectInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.dailyon.notificationservice.config.NotificationConfig.MEMBER_NOTIFICATION_CONNECTION_CHANNEL;
import static com.dailyon.notificationservice.config.NotificationConfig.UNIQUE_CONSUMER_IDENTIFIER;


@Slf4j
@Service
public class RedisPubSubService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    public RedisPubSubService(
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            SseNotificationService sseNotificationService,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = Objects.requireNonNull(reactiveRedisTemplate);
        this.sseNotificationService = Objects.requireNonNull(sseNotificationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);

        // 빈 생성하면서 구독 시작
        log.info("RedisPubSubService 빈을 생성하면서 구독 시작합니다.");
        subscribeToMemberConnectionChannel();
    }

    private void subscribeToMemberConnectionChannel() {
        log.info("redis sub 시작합니다.");
        reactiveRedisTemplate.listenTo(ChannelTopic.of(MEMBER_NOTIFICATION_CONNECTION_CHANNEL))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnNext(this::handleMemberDisconnection)
                .subscribe();
//                .subscribe(this::handleMemberDisconnection);
    }

    private void handleMemberDisconnection(String disconnectInfoDtoJson) {
        try {
            DisconnectInfoDto disconnectInfoDto = objectMapper.readValue(disconnectInfoDtoJson, DisconnectInfoDto.class);
            if (disconnectInfoDto.getUniqueConsumerName().equals(UNIQUE_CONSUMER_IDENTIFIER)) {
                log.info("동일 인스턴스에서 보낸 연결해제 pub입니다.");
                return;
            }

            Long memberIdToDisconnect = disconnectInfoDto.getMemberId();

            if (sseNotificationService.isUserConnected(memberIdToDisconnect)) {
                log.info(memberIdToDisconnect + "의 연결을 해제합니다.");
                sseNotificationService.disconnectMember(memberIdToDisconnect);
            }
        } catch (JsonProcessingException e) {
            log.error("handleMemberDisconnection의 readValue 도중 에러발생", e);
        }
    }

    public Mono<Void> publishMemberConnection(Long memberId) {
        log.info(memberId + "접속. disconnect 메세지 발행합니다.");
        DisconnectInfoDto disconnectInfoDto = DisconnectInfoDto.create(memberId);

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(disconnectInfoDto))
                .flatMap(jsonData -> reactiveRedisTemplate.convertAndSend(MEMBER_NOTIFICATION_CONNECTION_CHANNEL, jsonData))
                .doOnError(e -> {
                    log.error("publishMemberConnection 도중 에러발생", e);
                })
                .then();
    }
}
