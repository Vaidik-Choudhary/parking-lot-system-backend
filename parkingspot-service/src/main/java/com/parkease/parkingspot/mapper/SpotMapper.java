package com.parkease.parkingspot.mapper;

import org.springframework.stereotype.Component;

import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.ParkingSpot;
import com.parkease.parkingspot.entity.SpotStatus;

@Component
public class SpotMapper {

    public ParkingSpot toEntity(SpotRequestDTO dto) {
        return ParkingSpot.builder()
                .lotId(dto.getLotId())
                .spotNumber(dto.getSpotNumber())
                .floor(dto.getFloor())
                .spotType(dto.getSpotType())
                .vehicleType(dto.getVehicleType())
                .status(SpotStatus.AVAILABLE)
                .isEVCharging(dto.isEVCharging())
                .isHandicapped(dto.isHandicapped())
                .pricePerHour(dto.getPricePerHour())
                .build();
    }

    public SpotResponseDTO toDTO(ParkingSpot spot) {
        return SpotResponseDTO.builder()
                .spotId(spot.getSpotId())
                .lotId(spot.getLotId())
                .spotNumber(spot.getSpotNumber())
                .floor(spot.getFloor())
                .spotType(spot.getSpotType())
                .vehicleType(spot.getVehicleType())
                .status(spot.getStatus())
                .isEVCharging(spot.isEVCharging())
                .isHandicapped(spot.isHandicapped())
                .pricePerHour(spot.getPricePerHour())
                .createdAt(spot.getCreatedAt())
                .build();
    }
}
