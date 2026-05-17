package com.parkease.payment.dto.request;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPaymentRequest extends BasePaymentRequest {
    private String driverEmail;
}
