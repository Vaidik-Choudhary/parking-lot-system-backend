package com.parkease.booking.dto.request;

import com.parkease.booking.entity.BookingType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotNull(message = "Spot ID is required")
    private Long spotId;

    @NotBlank(message = "Vehicle plate is required")
    private String vehiclePlate;

    @NotNull(message = "Booking type is required (PRE_BOOKING or WALK_IN)")
    private BookingType bookingType;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
