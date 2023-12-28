package com.dailyon.notificationservice.config;

import com.dailyon.notificationservice.domain.notification.document.NotificationTemplate;
import com.dailyon.notificationservice.domain.notification.document.enums.NotificationType;
import com.dailyon.notificationservice.domain.notification.document.RestockNotification;
import com.dailyon.notificationservice.domain.notification.document.UserNotification;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

@Configuration
@Profile("local")
public class DataInitializerConfig {

    @Bean
    CommandLineRunner initData(ReactiveMongoTemplate template) {
        return args -> {
            NotificationTemplate template1 = NotificationTemplate.builder()
                    .message("0000 아이템이 재입고되었습니다.")
                    .linkUrl("http://example.com/product/12345")
                    .notificationType(NotificationType.PRODUCT_RESTOCK)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            RestockNotification restockNotification1 = RestockNotification.builder()
                    .productId("12345")
                    .sizeId("S")
                    .memberIds(Arrays.asList(101L, 102L, 103L))
                    .build();

            // Reactive operations는 non-blocking이라서 subscribe call이 필요함.
            // NotificationTemplate 객체 저장 후, UserNotification에 받아온 NotificationTemplate id를 넣음
            template.save(template1).flatMap(savedTemplate -> {
                HashSet<String> unreadNotifications = new HashSet<>();
                unreadNotifications.add(savedTemplate.getId());

                UserNotification userNotification1 = UserNotification.builder()
                        .memberId(1L)
                        .createdAt(Instant.now())
                        .unread(unreadNotifications)
                        .build();

                return template.save(userNotification1);
            }).subscribe();

            // Save the RestockNotification object
            template.save(restockNotification1).subscribe();
        };
    }
}