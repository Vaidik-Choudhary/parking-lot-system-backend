package com.parkease.vehicle.mapper;

import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;
 
@Component
public class VehicleMapper {
 
    public Vehicle toEntity(VehicleRequestDTO dto, String ownerEmail) {
        return Vehicle.builder()
                .ownerEmail(ownerEmail)
                .licensePlate(dto.getLicensePlate().toUpperCase().trim())
                .make(dto.getMake())
                .model(dto.getModel())
                .color(dto.getColor())
                .vehicleType(dto.getVehicleType())
                .isEV(dto.isEV())
                .isActive(true)
                .build();
    }
 
    public VehicleResponseDTO toDTO(Vehicle v) {
        return VehicleResponseDTO.builder()
                .vehicleId(v.getVehicleId())
                .ownerEmail(v.getOwnerEmail())
                .licensePlate(v.getLicensePlate())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .vehicleType(v.getVehicleType())
                .isEV(v.isEV())
                .isActive(v.isActive())
                .registeredAt(v.getRegisteredAt())
                .build();
    }
}
