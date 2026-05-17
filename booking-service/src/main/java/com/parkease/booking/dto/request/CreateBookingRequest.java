package com.parkease.booking.dto.request;

import com.parkease.booking.entity.BookingType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Request payload for creating a new booking.
 *
 * Booking type rules:
 *  - PRE_BOOKING : startTime must be in the future; endTime > startTime.
 *                  System validates overlap with existing reservations.
 *  - DRIVE_IN    : startTime is set to now() by the service layer (any value
 *                  sent is ignored/overridden). Only endTime (estimated
 *                  departure) needs to be provided and must be in the future.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotNull(message = "Spot ID is required")
    private Long spotId;

    @NotBlank(message = "Vehicle plate is required")
    private String vehiclePlate;

    /**
     * PRE_BOOKING or DRIVE_IN.
     * This is the first thing the user chooses on the frontend before seeing slots.
     */
    @NotNull(message = "Booking type is required (PRE_BOOKING or DRIVE_IN)")
    private BookingType bookingType;

    /**
     * For PRE_BOOKING: must be a future timestamp.
     * For DRIVE_IN   : ignored â€” overridden to LocalDateTime.now() in the service.
     */
    private LocalDateTime startTime;

    /**
     * Estimated end/departure time.
     * Must be after startTime (validated in service layer).
     */
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
