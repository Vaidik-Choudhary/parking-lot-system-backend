package com.parkease.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExtendBookingRequest {

    @NotNull(message = "New end time is required")
    @Future(message = "New end time must be in the future")
    private LocalDateTime newEndTime;
}
