package com.dailyon.notificationservice.domain.notification.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.dailyon.notificationservice.config.NotificationConfig.UNIQUE_CONSUMER_IDENTIFIER;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisconnectInfoDto {
    private Long memberId;
    private String uniqueConsumerName;

    public static DisconnectInfoDto create(Long memberId) {
        return DisconnectInfoDto.builder()
                .memberId(memberId)
                .uniqueConsumerName(UNIQUE_CONSUMER_IDENTIFIER)
                .build();
    }
}
