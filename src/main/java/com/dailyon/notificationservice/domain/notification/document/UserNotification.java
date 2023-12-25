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
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_notification")
public class UserNotification {

    @Id
    private String id;

    @Indexed
    private Long memberId;

    @CreatedDate
    private Instant createdAt;

    private List<String> unread;

    private List<String> read;

    private List<String> deleted;
}