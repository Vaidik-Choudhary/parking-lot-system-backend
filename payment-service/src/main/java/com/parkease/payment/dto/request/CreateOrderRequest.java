package com.parkease.payment.dto.request;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateOrderRequest extends BasePaymentRequest {
    private Long bookingId;
}
