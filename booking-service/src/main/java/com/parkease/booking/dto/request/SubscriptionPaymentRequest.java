package com.parkease.booking.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionPaymentRequest {
    private String driverEmail;
    private Long subscriptionId;
    private Double amount;
    private String description;
}
