package com.parkease.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CreateOrderRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
    private double amount;

    private String description;
}
