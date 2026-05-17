package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Map;

/**
 * Feign client for parkingspot-service.
 * Used by booking-service to:
 *  1. Fetch spot details (price, metadata)
 *  2. Change spot status (reserve / occupy / release)
 *  3. Fetch all spots for a lot (for pre-booking filter & drive-in view)
 */
@FeignClient(name = "PARKINGSPOT-SERVICE")
public interface SpotServiceClient {

    /** Fetch a single spot's details (including pricePerHour). */
    @GetMapping("/api/spots/{spotId}")
    Map<String, Object> getSpotById(@PathVariable("spotId") Long spotId);

    /**
     * Fetch ALL spots for a given lot.
     * Used by:
     *  - getAvailableSpotsForPreBooking() to filter by booked-spot-ids
     *  - getDriveInSpotView() to build the real-time availability grid
     */
    @GetMapping("/api/spots/lot/{lotId}")
    List<Map<String, Object>> getSpotsByLot(@PathVariable("lotId") Long lotId);

    /** Mark a spot RESERVED (called on pre-booking creation). */
    @PutMapping("/api/spots/{spotId}/reserve")
    void reserveSpot(@PathVariable("spotId") Long spotId);

    /** Mark a spot OCCUPIED (called on check-in). */
    @PutMapping("/api/spots/{spotId}/occupy")
    void occupySpot(@PathVariable("spotId") Long spotId);

    /** Mark a spot AVAILABLE (called on check-out / cancellation). */
    @PutMapping("/api/spots/{spotId}/release")
    void releaseSpot(@PathVariable("spotId") Long spotId);
}
