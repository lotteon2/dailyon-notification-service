package com.dailyon.notificationservice.domain.notification.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "restock_notification")
@CompoundIndex(name = "restock_notification_idx", def = "{'productId' : 1, 'sizeId' : 1}", unique = true)
public class RestockNotification {

    @Id
    private String id;

    // 유일한 복합키로 관리
    private Long productId;
    private Long sizeId;

    // 재입고 신청한 memberIds
    @Builder.Default private Set<Long> memberIds = new HashSet<>(); // document 생성시 not null 보장;
}