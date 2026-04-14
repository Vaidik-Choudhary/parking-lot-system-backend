package com.parkease.vehicle.controller;
 
import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.ApiResponse;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {
 
    private final VehicleService service;
 
    @PostMapping
    public ResponseEntity<VehicleResponseDTO> register(
            @Valid @RequestBody VehicleRequestDTO dto,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("POST /api/vehicles - driver: {}", email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registerVehicle(dto, email));
    }
 
    @GetMapping("/my")
    public ResponseEntity<List<VehicleResponseDTO>> getMyVehicles(Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("GET /api/vehicles/my - driver: {}", email);
        return ResponseEntity.ok(service.getMyVehicles(email));
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getVehicleById(id));
    }
 
    @GetMapping("/plate/{plate}")
    public ResponseEntity<VehicleResponseDTO> getByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(service.getByLicensePlate(plate));
    }
 
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequestDTO dto,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/vehicles/{} - driver: {}", id, email);
        return ResponseEntity.ok(service.updateVehicle(id, dto, email));
    }
 
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("DELETE /api/vehicles/{} - driver: {}", id, email);
        service.deleteVehicle(id, email);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle deleted successfully."));
    }
 
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivate(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/vehicles/{}/deactivate - driver: {}", id, email);
        service.deactivateVehicle(id, email);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle deactivated."));
    }
}
