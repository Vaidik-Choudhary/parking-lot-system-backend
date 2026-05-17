package com.parkease.parkingspot.dto.response;

import java.time.LocalDateTime;

import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full spot detail returned to callers.
 *
 * availableUntil â€“ Optional field populated by booking-service (not this service)
 *                  during drive-in views to indicate when the next pre-booking
 *                  starts. Null means the spot is fully free.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotResponseDTO {

    private Long        spotId;
    private Long        lotId;
    private String      spotNumber;
    private int         floor;
    private SpotType    spotType;
    private VehicleType vehicleType;
    private SpotStatus  status;
    private boolean     isEVCharging;
    private boolean     isHandicapped;
    private double      pricePerHour;
    private boolean     monthlySubscriptionEnabled;
    private Double      monthlyRate;
    private LocalDateTime createdAt;

    /**
     * Populated externally (by booking-service) for drive-in views only.
     * Indicates when the next pre-booking reservation begins on this spot.
     * Null when the spot has no upcoming reservations.
     */
    private LocalDateTime availableUntil;
}
