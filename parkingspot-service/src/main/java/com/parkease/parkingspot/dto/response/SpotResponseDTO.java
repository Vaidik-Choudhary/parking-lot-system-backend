package com.parkease.parkingspot.dto.response;

import java.time.LocalDateTime;

import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class SpotResponseDTO {

    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private int floor;
    private SpotType spotType;
    private VehicleType vehicleType;
    private SpotStatus status;
    private boolean isEVCharging;
    private boolean isHandicapped;
    private double pricePerHour;
    private LocalDateTime createdAt;
}
