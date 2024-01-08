package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.common.exceptions.ErrorResponseException;
import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import com.dailyon.notificationservice.domain.notification.document.RestockNotification;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import com.dailyon.notificationservice.domain.notification.dto.NotificationData;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.dailyon.notificationservice.domain.notification.repository.RestockNotificationRepository;
import com.dailyon.notificationservice.domain.notification.repository.UserNotificationRepository;

import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SseNotificationService {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final RestockNotificationRepository restockNotificationRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    private final Map<Long, Sinks.Many<ServerSentEvent<NotificationData>>> userSinks = new ConcurrentHashMap<>();
    // ConcurrentHashMap을 쓰는것과 Map으로 선언하는것의 차이?

    // 구독하기. 구독 SSE 객체에는 client에게 줄 notificationData를 넣어준다.
    public Flux<ServerSentEvent<NotificationData>> streamNotifications(Long memberId) {
        Sinks.Many<ServerSentEvent<NotificationData>> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.put(memberId, sink);

        Consumer<Throwable> removeSinkConsumer = e -> userSinks.remove(memberId);

        return sink.asFlux()
                .doOnCancel(() -> removeSinkConsumer.accept(null))
                .doOnError(removeSinkConsumer);
    }



    // DB에 알림 template 저장 후, UserNotifications업데이트, SSE 송출.
    public Mono<Void> onNotificationReceived(NotificationData data, List<Long> memberIds) {
        // NotificationTemplate 저장 우선
        return notificationTemplateRepository.save(
                        NotificationTemplate.builder()
                                .message(data.getMessage())
                                .linkUrl(data.getLinkUrl())
                                .notificationType(data.getNotificationType())
                                .build()
                )
                .flatMap(savedTemplate -> {
                    if (memberIds != null && !memberIds.isEmpty()) {
                        // 대상들의 UserNotifications document 업데이트, SSE 송출
                        return updateMultipleUserNotifications(memberIds, savedTemplate.getId())
                                .thenMany(Flux.fromIterable(memberIds))
                                .flatMap(memberId -> sendSseNotificationToUser(NotificationData.from(savedTemplate, false), memberId))
                                .then();
                    } else {
                        // 가공 이후에도 memberIds null/empty라면 현재 연결된 모든 유저들에게 나가는 알림이라는 뜻.
                        return sendSseNotificationToAllUsers(NotificationData.from(savedTemplate, false));
                    }
                });
    }


    private Mono<Void> updateMultipleUserNotifications(List<Long> memberIds, String notificationTemplateId) {
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



    private Mono<Void> sendSseNotificationToUser(NotificationData data, Long memberId) {
        return Mono.fromRunnable(() -> {
            Optional.ofNullable(userSinks.get(memberId)).ifPresent(sink -> {
                sink.tryEmitNext(ServerSentEvent.builder(data)
                    .event("notification-event")
                    .build())
                    .orThrow();
            });
        });
    }

    private Mono<Void> sendSseNotificationToAllUsers(NotificationData data) {
        return Flux.fromIterable(new ArrayList<>(userSinks.keySet()))
                .flatMap(memberId -> sendSseNotificationToUser(data, memberId)).then();
    }

    // TODO: 이 코드가 여기 있는게 맞을까요.. api로 호출되는것도 아니고.. 따로 파일 빼기도 애매하고
    public Mono<Void> clearProductRestockNotifications(Long productId, Long sizeId) {
        return restockNotificationRepository.findByProductIdAndSizeId(productId, sizeId)
                .flatMap(restockNotification -> {
                    restockNotification.getMemberIds().clear();
                    return restockNotificationRepository.save(restockNotification);
                }).then();
    }

}
