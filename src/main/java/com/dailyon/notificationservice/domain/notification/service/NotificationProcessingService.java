package com.dailyon.notificationservice.domain.notification.service;


import com.dailyon.notificationservice.config.NotificationConfig;
import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import com.dailyon.notificationservice.domain.notification.dto.*;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {
    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseNotificationService sseNotificationService;
    private final NotificationUtils notificationUtils;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public Mono<Void> processMessage(String message) {
        return Mono.just(message)
                .flatMap(this::extractNotificationData)
                .flatMap(this::processExtendedNotification)
                .onErrorResume(JsonProcessingException.class, e ->
                        Mono.error(new RuntimeException("JSON message 처리 중 에러: ", e))
                );
    }

    private Mono<ExtendedNotificationData> extractNotificationData(String message) {
        return Mono.fromCallable(() -> {
            SQSNotificationDto sqsNotificationDto = objectMapper.readValue(message, SQSNotificationDto.class);
            RawNotificationData rawNotificationData = sqsNotificationDto.getRawNotificationData();
            NotificationData notificationData = NotificationData.fromRawData(rawNotificationData);
            return ExtendedNotificationData.of(sqsNotificationDto.getWhoToNotify(), notificationData, rawNotificationData.getParameters());
        }).onErrorMap(JsonProcessingException.class, e ->
                new RuntimeException("JSON message 처리 중 에러: ", e)
        );
    }

    public Mono<Void> processExtendedNotification(ExtendedNotificationData extendedData) {
        return notificationUtils.determineMemberIds(
                extendedData.getNotificationData().getNotificationType(),
                extendedData.getParameters(),
                extendedData.getWhoToNotify()
        )
                .flatMap(memberIds -> saveNotificationTemplate(extendedData.getNotificationData())
                        .flatMap(savedTemplate -> {
                            NotificationData updatedNotificationData = NotificationData.from(savedTemplate, false);
                            return updateMultipleUserNotifications(memberIds, savedTemplate.getId())
                                    .then(sendNotifications(memberIds, updatedNotificationData));
                        })
                );
    }

    private Mono<Void> sendNotifications(List<Long> memberIds, NotificationData notificationData) {
        List<Long> connectedMemberIds = memberIds.stream()
                .filter(sseNotificationService::isUserConnected)
                .collect(Collectors.toList());

        Mono<Void> connectedUsersMono = sseNotificationService.sendNotificationToConnectedUsers(connectedMemberIds, notificationData);
        List<Long> unconnectedMemberIds = memberIds.stream()
                .filter(id -> !sseNotificationService.isUserConnected(id))
                .collect(Collectors.toList());

        Mono<Void> publishToStreamMono = publishUnconnectedUserNotifications(unconnectedMemberIds, notificationData);

        // connectedUsersMono, publishToStreamMono가 다 끝나야 Mono 반환
        return Mono.when(connectedUsersMono, publishToStreamMono);
    }

    public Mono<Void> publishUnconnectedUserNotifications(List<Long> unconnectedMemberIds, NotificationData notificationData) {
        if (unconnectedMemberIds.isEmpty()) {
            return Mono.empty(); // 비어있으니 무시
        }
//        log.info("publishUnconnectedUserNotifications 입장 - 미연결 memberIds: {} \n 데이터: {}", unconnectedMemberIds, notificationData);
        return Mono.fromCallable(() -> {
                    NotificationDataWithWhoToNotify redisNotificationDto = NotificationDataWithWhoToNotify.create(unconnectedMemberIds, notificationData);
                    return objectMapper.writeValueAsString(redisNotificationDto);
                })
                .flatMap(jsonNotification -> stringRedisTemplate.opsForStream()
                        .add(StreamRecords.newRecord()
                                .in(NotificationConfig.NOTIFICATIONS_STREAM_KEY)
                                .ofObject(jsonNotification))
                )
                .then();
    }

    private Mono<NotificationTemplate> saveNotificationTemplate(NotificationData notificationData) {
        return notificationTemplateRepository.save(NotificationTemplate.builder()
                .message(notificationData.getMessage())
                .linkUrl(notificationData.getLinkUrl())
                .notificationType(notificationData.getNotificationType())
                .build());
    }

    private Mono<Void> updateMultipleUserNotifications(List<Long> memberIds, String notificationTemplateId) {
//        log.info("Starting bulk update for UserNotification with templateId: {}", notificationTemplateId);
        // 'unread' 필드에 notificationTemplateId를 추가하는 BSON 업데이트 정의
        Document updateDocument = new Document("$addToSet", new Document("unread", notificationTemplateId));

        // 'user_notification' 컬렉션이름 조회
        String collectionName = reactiveMongoTemplate.getCollectionName(UserNotification.class);

        // bulk operation 위해 memberIds 개수 만큼의 List<WriteModel<Document>> 생성
        List<WriteModel<Document>> bulkWriteModels = memberIds.stream().map(memberId -> {
            // 1. 순회하며 memberIds의 memberId 원소 필터 생성
            Document filter = new Document("memberId", memberId);

            // 2. UpdateOneModel<Document> 이용, WriteModel<Document>객체 리스트 생성 -> 필터에 걸린 document들 대상 bulkwrite 진행.
            return new UpdateOneModel<Document>(filter, updateDocument);
        }).collect(Collectors.toList());

        // 컬렉션을 비동기적으로 가져오기 위한 Mono<MongoCollection<Document>>
        Mono<MongoCollection<Document>> userNotificationCollectionMono = reactiveMongoTemplate.getCollection(collectionName);

        // collection.bulkWrite() 이용, MongoCollection에 대한 비동기 벌크 업데이트 진행.
        // This expects a list of WriteModel<Document>, which we provide.
        return userNotificationCollectionMono.flatMap(collection ->
                Mono.from(collection.bulkWrite(bulkWriteModels))
        ).then();
        // 개별 연산들을 서버에서 파이프라인을 통해 순차적으로 혹은 가능한 범위 내에서 병렬로 안전하게 실행
        // application 단위에서 직접 set에 넣고 save하는게 아니라 $addToSet notificationTemplateId 연산으로 정의하기 때문에 race condition을 막음.
    }
}
