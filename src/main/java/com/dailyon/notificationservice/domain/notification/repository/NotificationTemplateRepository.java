package com.dailyon.notificationservice.domain.notification.repository;

import org.springframework.stereotype.Repository;
import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Repository
public interface NotificationTemplateRepository extends ReactiveMongoRepository<NotificationTemplate, String> {
    Flux<NotificationTemplate> findByIdIn(Collection<String> ids);
}
