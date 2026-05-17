package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Feign client for vehicle-service.
 * Used by booking-service to:
 *  1. Validate that a vehicle plate is registered before creating a booking.
 *  2. Fetch vehicle details (vehicleType, isEV) for spot compatibility checks.
 */
@FeignClient(name = "VEHICLE-SERVICE")
public interface VehicleServiceClient {

    /** Fetch a vehicle by its license plate. */
    @GetMapping("/api/vehicles/plate/{plate}")
    Map<String, Object> getVehicleByPlate(@RequestHeader("Authorization") String token, @PathVariable("plate") String plate);
}
