package com.parkease.parkingspot.dto.request;

import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotRequestDTO {

    @NotNull(message = "Lot ID is required")
    private Long lotId;

    @NotBlank(message = "Spot number is required (e.g. A-01)")
    private String spotNumber;

    @Min(value = 0, message = "Floor must be 0 or above")
    private int floor;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private boolean isEVCharging = false;
    private boolean isHandicapped = false;

    @DecimalMin(value = "0.1", message = "Price per hour must be greater than 0")
    private double pricePerHour;
}
