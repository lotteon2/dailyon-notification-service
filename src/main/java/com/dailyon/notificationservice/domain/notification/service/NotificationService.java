package com.dailyon.notificationservice.domain.notification.service;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.repository.NotificationTemplateRepository;
import com.dailyon.notificationservice.domain.notification.repository.UserNotificationRepository;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ReactiveMongoOperations mongoOperations;

    
}
