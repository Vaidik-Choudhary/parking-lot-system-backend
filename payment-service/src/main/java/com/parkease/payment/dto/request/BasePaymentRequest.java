package com.parkease.payment.dto.request;

import lombok.*;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.*;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BasePaymentRequest {
    private Long subscriptionId;
    
    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private Double amount;
    
    private String description;
}
