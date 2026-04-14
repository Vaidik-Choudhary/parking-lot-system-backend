package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.entity.ParkingLot;
import com.parkease.parkinglot.exception.ResourceNotFoundException;
import com.parkease.parkinglot.exception.UnauthorizedException;
import com.parkease.parkinglot.mapper.ParkingLotMapper;
import com.parkease.parkinglot.repository.ParkingLotRepository;
import com.parkease.parkinglot.util.HaversineUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository repo;
    private final ParkingLotMapper mapper;

    private ParkingLot getLotOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot not found with id: " + id));
    }

    private void verifyOwnership(ParkingLot lot, String managerEmail) {
        if (lot.getManagerEmail() == null || !lot.getManagerEmail().equals(managerEmail)) {
            throw new UnauthorizedException("You do not have permission to modify this lot.");
        }
    }

    @Override
    @Transactional
    public ParkingLotResponseDTO createLot(ParkingLotRequestDTO dto, String managerEmail) {
        log.info("Creating lot '{}' for manager: {}", dto.getName(), managerEmail);
        ParkingLot lot = mapper.toEntity(dto, managerEmail);
        ParkingLot saved = repo.save(lot);
        log.info("Lot created with id: {} — pending admin approval", saved.getLotId());
        return mapper.toDTO(saved);
    }

    @Override
    public ParkingLotResponseDTO getLotById(Long id) {
        log.debug("Fetching lot by id: {}", id);
        return mapper.toDTO(getLotOrThrow(id));
    }

    @Override
    public List<ParkingLotResponseDTO> getByCity(String city) {
        log.debug("Fetching lots in city: {}", city);
        return repo.findByCityIgnoreCaseAndIsApprovedTrue(city)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ParkingLotResponseDTO> getNearbyLots(double lat, double lon, double radiusKm) {
        log.debug("Finding lots near ({}, {}) within {} km", lat, lon, radiusKm);

        List<ParkingLot> lots = repo.findNearby(lat, lon, radiusKm);

        return lots.stream()
                .map(lot -> {
                    double distance = HaversineUtil.calculateDistance(
                            lat, lon,
                            lot.getLatitude(), lot.getLongitude()
                    );
                    return mapper.toDTO(lot, distance);
                })
                .toList();
    }

    @Override
    public List<ParkingLotResponseDTO> getOpenLots() {
        return repo.findByIsOpenTrueAndIsApprovedTrue()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ParkingLotResponseDTO> getLotsByManager(String managerEmail) {
        log.debug("Fetching lots for manager: {}", managerEmail);
        return repo.findByManagerEmail(managerEmail)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public ParkingLotResponseDTO updateLot(Long id, ParkingLotRequestDTO dto, String managerEmail) {
        log.info("Updating lot id: {} by manager: {}", id, managerEmail);

        ParkingLot lot = getLotOrThrow(id);
        verifyOwnership(lot, managerEmail);

        lot.setName(dto.getName());
        lot.setAddress(dto.getAddress());
        lot.setCity(dto.getCity());
        lot.setLatitude(dto.getLatitude());
        lot.setLongitude(dto.getLongitude());
        lot.setTotalSpots(dto.getTotalSpots());
        lot.setOpenTime(dto.getOpenTime());
        lot.setCloseTime(dto.getCloseTime());
        lot.setImageUrl(dto.getImageUrl());

        return mapper.toDTO(repo.save(lot));
    }

    @Override
    @Transactional
    public void deleteLot(Long id, String managerEmail) {
        log.info("Deleting lot id: {} by manager: {}", id, managerEmail);
        ParkingLot lot = getLotOrThrow(id);
        verifyOwnership(lot, managerEmail);
        repo.delete(lot);
    }

    @Override
    @Transactional
    public ParkingLotResponseDTO toggleOpen(Long id, String managerEmail) {
        log.info("Toggling open status for lot id: {} by manager: {}", id, managerEmail);
        ParkingLot lot = getLotOrThrow(id);
        verifyOwnership(lot, managerEmail);

       
        if (!lot.isApproved()) {
            throw new UnauthorizedException("Lot is not approved yet. Contact admin.");
        }

        lot.setOpen(!lot.isOpen());
        String status = lot.isOpen() ? "OPEN" : "CLOSED";
        log.info("Lot id: {} is now {}", id, status);
        return mapper.toDTO(repo.save(lot));
    }

    @Override
    @Transactional
    public ParkingLotResponseDTO approveLot(Long id) {
        log.info("Admin approving lot id: {}", id);
        ParkingLot lot = getLotOrThrow(id);
        lot.setApproved(true);
        return mapper.toDTO(repo.save(lot));
    }

    @Override
    @Transactional
    public ParkingLotResponseDTO rejectLot(Long id) {
        log.info("Admin rejecting lot id: {}", id);
        ParkingLot lot = getLotOrThrow(id);
        lot.setApproved(false);
        lot.setOpen(false);
        return mapper.toDTO(repo.save(lot));
    }

    @Override
    public List<ParkingLotResponseDTO> getPendingApprovalLots() {
        log.debug("Fetching lots pending approval");
        return repo.findByIsApprovedFalse()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }


    @Override
    @Transactional  
    public void decrementSpot(Long lotId) {
        ParkingLot lot = getLotOrThrow(lotId);
        if (lot.getAvailableSpots() <= 0) {
            throw new IllegalStateException("No available spots in lot: " + lotId);
        }
        lot.setAvailableSpots(lot.getAvailableSpots() - 1);
        repo.save(lot);
        log.debug("Decremented spots for lot {}. Remaining: {}", lotId, lot.getAvailableSpots());
    }

    @Override
    @Transactional   
    public void incrementSpot(Long lotId) {
        ParkingLot lot = getLotOrThrow(lotId);
        if (lot.getAvailableSpots() >= lot.getTotalSpots()) {
            log.warn("Lot {} already at full capacity, skipping increment", lotId);
            return;
        }
        lot.setAvailableSpots(lot.getAvailableSpots() + 1);
        repo.save(lot);
        log.debug("Incremented spots for lot {}. Now: {}", lotId, lot.getAvailableSpots());
    }
}
