package com.dailyon.notificationservice.domain.notification.api.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class EnrollRestockRequest {
    @NotNull private Long productId;
    @NotNull private Long sizeId;
}
