package com.parkease.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubscriptionRequest {
    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotNull(message = "Spot ID is required")
    private Long spotId;
}
