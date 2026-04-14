package com.parkease.vehicle.service;
 
import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import java.util.List;
 
public interface VehicleService {
    VehicleResponseDTO registerVehicle(VehicleRequestDTO dto, String ownerEmail);
    VehicleResponseDTO updateVehicle(Long vehicleId, VehicleRequestDTO dto, String ownerEmail);
    void deleteVehicle(Long vehicleId, String ownerEmail);
    VehicleResponseDTO getVehicleById(Long vehicleId);
    List<VehicleResponseDTO> getMyVehicles(String ownerEmail);
    VehicleResponseDTO getByLicensePlate(String plate);
    void deactivateVehicle(Long vehicleId, String ownerEmail);
}