package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;

import java.util.List;

public interface ParkingLotService {

    // â”€â”€ Manager operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    ParkingLotResponseDTO createLot(ParkingLotRequestDTO dto, String managerEmail);
    ParkingLotResponseDTO updateLot(Long id, ParkingLotRequestDTO dto, String managerEmail);
    void deleteLot(Long id, String managerEmail);
    ParkingLotResponseDTO toggleOpen(Long id, String managerEmail);
    List<ParkingLotResponseDTO> getLotsByManager(String managerEmail);

    // â”€â”€ Public / Driver operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    ParkingLotResponseDTO getLotById(Long id);
    List<ParkingLotResponseDTO> getByCity(String city, Boolean hasEV, Boolean has2W, Boolean has4W, Boolean hasHeavy, Boolean hasHandicap);
    List<ParkingLotResponseDTO> getNearbyLots(com.parkease.parkinglot.dto.request.NearbySearchRequest searchReq);
    List<ParkingLotResponseDTO> getOpenLots();

    // â”€â”€ Admin operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    ParkingLotResponseDTO approveLot(Long id);
    ParkingLotResponseDTO rejectLot(Long id);
    List<ParkingLotResponseDTO> getPendingApprovalLots();

    // â”€â”€ Internal (called by booking-service) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    void decrementSpot(Long lotId);
    void incrementSpot(Long lotId);
}
