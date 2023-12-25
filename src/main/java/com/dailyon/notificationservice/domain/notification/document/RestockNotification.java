package com.dailyon.notificationservice.domain.notification.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "restock_notification")
@CompoundIndex(name = "restock_notification_idx", def = "{'productId' : 1, 'sizeId' : 1}", unique = true)
public class RestockNotification {

    @Id
    private String id;

    private String productId;

    private String sizeId;

    private List<Long> memberIds;
}