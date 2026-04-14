package com.parkease.parkingspot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.parkingspot.dto.request.BulkSpotRequestDTO;
import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.ApiResponse;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;
import com.parkease.parkingspot.service.SpotService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
@Slf4j
public class SpotController {

    private final SpotService service;

    @GetMapping("/{spotId}")
    public ResponseEntity<SpotResponseDTO> getById(@PathVariable Long spotId) {
        return ResponseEntity.ok(service.getSpotById(spotId));
    }

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<SpotResponseDTO>> getByLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getSpotsByLot(lotId));
    }

    @GetMapping("/lot/{lotId}/available")
    public ResponseEntity<List<SpotResponseDTO>> getAvailable(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getAvailableSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/floor/{floor}")
    public ResponseEntity<List<SpotResponseDTO>> getByFloor(
            @PathVariable Long lotId, @PathVariable int floor) {
        return ResponseEntity.ok(service.getSpotsByFloor(lotId, floor));
    }

    @GetMapping("/lot/{lotId}/type/{spotType}")
    public ResponseEntity<List<SpotResponseDTO>> getByType(
            @PathVariable Long lotId, @PathVariable SpotType spotType) {
        return ResponseEntity.ok(service.getSpotsByType(lotId, spotType));
    }

    @GetMapping("/lot/{lotId}/vehicle/{vehicleType}")
    public ResponseEntity<List<SpotResponseDTO>> getByVehicleType(
            @PathVariable Long lotId, @PathVariable VehicleType vehicleType) {
        return ResponseEntity.ok(service.getSpotsByVehicleType(lotId, vehicleType));
    }

    @GetMapping("/lot/{lotId}/ev")
    public ResponseEntity<List<SpotResponseDTO>> getEVSpots(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getEVSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/handicapped")
    public ResponseEntity<List<SpotResponseDTO>> getHandicappedSpots(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getHandicappedSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/count")
    public ResponseEntity<Integer> getAvailableCount(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.countAvailableSpots(lotId));
    }

    
    
    

    @PostMapping
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<SpotResponseDTO> addSpot(@Valid @RequestBody SpotRequestDTO dto) {
        log.info("POST /api/spots - lot: {}", dto.getLotId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addSpot(dto));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<List<SpotResponseDTO>> addBulk(@Valid @RequestBody BulkSpotRequestDTO dto) {
        log.info("POST /api/spots/bulk - lot: {}, count: {}", dto.getLotId(), dto.getCount());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addBulkSpots(dto));
    }

    @PutMapping("/{spotId}")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<SpotResponseDTO> updateSpot(
            @PathVariable Long spotId, @Valid @RequestBody SpotRequestDTO dto) {
        log.info("PUT /api/spots/{}", spotId);
        return ResponseEntity.ok(service.updateSpot(spotId, dto));
    }

    @DeleteMapping("/{spotId}")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<ApiResponse> deleteSpot(@PathVariable Long spotId) {
        log.info("DELETE /api/spots/{}", spotId);
        service.deleteSpot(spotId);
        return ResponseEntity.ok(ApiResponse.ok("Spot deleted successfully."));
    }

    @PutMapping("/{spotId}/maintenance")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<SpotResponseDTO> setMaintenance(@PathVariable Long spotId) {
        log.info("PUT /api/spots/{}/maintenance", spotId);
        return ResponseEntity.ok(service.setMaintenance(spotId));
    }

    
    
    
    

    @PutMapping("/{spotId}/reserve")
    public ResponseEntity<SpotResponseDTO> reserve(@PathVariable Long spotId) {
        log.debug("PUT /api/spots/{}/reserve", spotId);
        return ResponseEntity.ok(service.reserveSpot(spotId));
    }

    @PutMapping("/{spotId}/occupy")
    public ResponseEntity<SpotResponseDTO> occupy(@PathVariable Long spotId) {
        log.debug("PUT /api/spots/{}/occupy", spotId);
        return ResponseEntity.ok(service.occupySpot(spotId));
    }

    @PutMapping("/{spotId}/release")
    public ResponseEntity<SpotResponseDTO> release(@PathVariable Long spotId) {
        log.debug("PUT /api/spots/{}/release", spotId);
        return ResponseEntity.ok(service.releaseSpot(spotId));
    }
}
