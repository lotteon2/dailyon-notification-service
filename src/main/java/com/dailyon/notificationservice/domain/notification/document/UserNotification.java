package com.dailyon.notificationservice.domain.notification.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_notification")
public class UserNotification {

    @Id
    private String id;

    // 알림 연결시 memberId로 찾기 위해 index 추가
    @Indexed
    private Long memberId;

    @CreatedDate
    private Instant createdAt;

    // NotificationTemplate의 id를 hashSet로 가짐. unread, read, deleted로 관리
    @Builder.Default private Set<String> unread = new HashSet<>(); // document 생성시 not null 보장
    @Builder.Default private Set<String> read = new HashSet<>();
    @Builder.Default private Set<String> deleted = new HashSet<>();

    public void markAsRead(String notificationId) {
        unread.remove(notificationId);
        read.add(notificationId);
    }

    // unread -> read
    public void markAsReadAllNotifications() {
        read.addAll(unread);
        unread.clear();
    }

    public void deleteNotification(String notificationId) {
        unread.remove(notificationId);
        read.remove(notificationId);
        deleted.add(notificationId);
    }

    public void deleteAllNotifications() {
        deleted.addAll(unread);
        deleted.addAll(read);
        unread.clear();
        read.clear();
    }
}