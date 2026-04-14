package com.parkease.parkingspot.service;

import java.util.List;

import com.parkease.parkingspot.dto.request.BulkSpotRequestDTO;
import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;

public interface SpotService {

    // ── Manager operations ────────────────────────────────────────────────────
    SpotResponseDTO addSpot(SpotRequestDTO dto);
    List<SpotResponseDTO> addBulkSpots(BulkSpotRequestDTO dto);
    SpotResponseDTO updateSpot(Long spotId, SpotRequestDTO dto);
    void deleteSpot(Long spotId);

    // ── Query operations ──────────────────────────────────────────────────────
    SpotResponseDTO getSpotById(Long spotId);
    List<SpotResponseDTO> getSpotsByLot(Long lotId);
    List<SpotResponseDTO> getAvailableSpots(Long lotId);
    List<SpotResponseDTO> getSpotsByType(Long lotId, SpotType spotType);
    List<SpotResponseDTO> getSpotsByVehicleType(Long lotId, VehicleType vehicleType);
    List<SpotResponseDTO> getSpotsByFloor(Long lotId, int floor);
    List<SpotResponseDTO> getEVSpots(Long lotId);
    List<SpotResponseDTO> getHandicappedSpots(Long lotId);
    int countAvailableSpots(Long lotId);

    // ── Status transitions (called by booking-service) ────────────────────────
    SpotResponseDTO reserveSpot(Long spotId);    
    SpotResponseDTO occupySpot(Long spotId);  
    SpotResponseDTO releaseSpot(Long spotId);   
    SpotResponseDTO setMaintenance(Long spotId); 
}
