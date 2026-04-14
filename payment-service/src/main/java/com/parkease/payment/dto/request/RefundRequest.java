package com.parkease.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String reason;
}
