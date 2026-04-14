package com.parkease.parkinglot.controller;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ApiResponse;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.service.ParkingLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
@Slf4j
public class ParkingLotController {

    private final ParkingLotService service;

    /**
     * GET /api/lots
     * Returns all open + approved lots.
     * Guests and drivers can browse without logging in.
     */
    @GetMapping
    public ResponseEntity<List<ParkingLotResponseDTO>> getAllOpenLots() {
        log.info("GET /api/lots");
        return ResponseEntity.ok(service.getOpenLots());
    }

    /**
     * GET /api/lots/{id}
     * Get a single lot by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParkingLotResponseDTO> getLotById(@PathVariable Long id) {
        log.info("GET /api/lots/{}", id);
        return ResponseEntity.ok(service.getLotById(id));
    }

    /**
     * GET /api/lots/city/{city}
     * Get all approved lots in a specific city.
     * Example: GET /api/lots/city/Mumbai
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<ParkingLotResponseDTO>> getLotsByCity(@PathVariable String city) {
        log.info("GET /api/lots/city/{}", city);
        return ResponseEntity.ok(service.getByCity(city));
    }

    /**
     * GET /api/lots/nearby?lat=18.9&lon=72.8&radius=5
     * Find all approved+open lots within a given radius (km) of coordinates.
     *
     * Uses the Haversine formula — results include distanceKm field, sorted nearest first.
     *
     * Params:
     *   lat    = user's latitude
     *   lon    = user's longitude
     *   radius = search radius in km (default: 5 km)
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<ParkingLotResponseDTO>> getNearbyLots(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radius) {
        log.info("GET /api/lots/nearby - lat={}, lon={}, radius={}km", lat, lon, radius);
        return ResponseEntity.ok(service.getNearbyLots(lat, lon, radius));
    }

    /**
     * POST /api/lots
     * Create a new parking lot. Starts as unapproved — admin must approve it.
     * The manager's email is extracted from the JWT automatically.
     */
    @PostMapping
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<ParkingLotResponseDTO> createLot(
            @Valid @RequestBody ParkingLotRequestDTO dto,
            Authentication auth) {

        // FIX: principal is email String from JWT — NOT Long (your original bug)
        String managerEmail = (String) auth.getPrincipal();
        log.info("POST /api/lots - manager: {}", managerEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createLot(dto, managerEmail));
    }

    /**
     * PUT /api/lots/{id}
     * Update an existing lot. Only the owner manager can do this.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<ParkingLotResponseDTO> updateLot(
            @PathVariable Long id,
            @Valid @RequestBody ParkingLotRequestDTO dto,
            Authentication auth) {

        String managerEmail = (String) auth.getPrincipal();
        log.info("PUT /api/lots/{} - manager: {}", id, managerEmail);
        return ResponseEntity.ok(service.updateLot(id, dto, managerEmail));
    }

    /**
     * DELETE /api/lots/{id}
     * Delete a lot. Only the owner manager can do this.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<ApiResponse> deleteLot(
            @PathVariable Long id,
            Authentication auth) {

        String managerEmail = (String) auth.getPrincipal();
        log.info("DELETE /api/lots/{} - manager: {}", id, managerEmail);
        service.deleteLot(id, managerEmail);
        return ResponseEntity.ok(ApiResponse.ok("Lot deleted successfully."));
    }

    /**
     * PUT /api/lots/{id}/toggle
     * Toggle lot open/closed status. Only works if lot is approved.
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<ParkingLotResponseDTO> toggleOpen(
            @PathVariable Long id,
            Authentication auth) {

        String managerEmail = (String) auth.getPrincipal();
        log.info("PUT /api/lots/{}/toggle - manager: {}", id, managerEmail);
        return ResponseEntity.ok(service.toggleOpen(id, managerEmail));
    }

    /**
     * GET /api/lots/my-lots
     * Get all lots belonging to the currently logged-in manager.
     */
    @GetMapping("/my-lots")
    @PreAuthorize("hasRole('LOT_MANAGER')")
    public ResponseEntity<List<ParkingLotResponseDTO>> getMyLots(Authentication auth) {
        String managerEmail = (String) auth.getPrincipal();
        log.info("GET /api/lots/my-lots - manager: {}", managerEmail);
        return ResponseEntity.ok(service.getLotsByManager(managerEmail));
    }

    /**
     * GET /api/lots/admin/pending
     * Get all lots waiting for admin approval.
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParkingLotResponseDTO>> getPendingLots() {
        log.info("GET /api/lots/admin/pending");
        return ResponseEntity.ok(service.getPendingApprovalLots());
    }

    /**
     * PUT /api/lots/admin/{id}/approve
     * Approve a lot — it becomes publicly searchable after this.
     */
    @PutMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingLotResponseDTO> approveLot(@PathVariable Long id) {
        log.info("PUT /api/lots/admin/{}/approve", id);
        return ResponseEntity.ok(service.approveLot(id));
    }

    /**
     * PUT /api/lots/admin/{id}/reject
     * Reject a lot registration.
     */
    @PutMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingLotResponseDTO> rejectLot(@PathVariable Long id) {
        log.info("PUT /api/lots/admin/{}/reject", id);
        return ResponseEntity.ok(service.rejectLot(id));
    }

    /**
     * PUT /api/lots/{id}/decrement
     * Called by booking-service when a spot is booked.
     * Reduces availableSpots by 1.
     */
    @PutMapping("/{id}/decrement")
    public ResponseEntity<ApiResponse> decrementSpot(@PathVariable Long id) {
        log.debug("PUT /api/lots/{}/decrement", id);
        service.decrementSpot(id);
        return ResponseEntity.ok(ApiResponse.ok("Available spots decremented."));
    }

    /**
     * PUT /api/lots/{id}/increment
     * Called by booking-service on checkout or cancellation.
     * Increases availableSpots by 1.
     */
    @PutMapping("/{id}/increment")
    public ResponseEntity<ApiResponse> incrementSpot(@PathVariable Long id) {
        log.debug("PUT /api/lots/{}/increment", id);
        service.incrementSpot(id);
        return ResponseEntity.ok(ApiResponse.ok("Available spots incremented."));
    }
}
