package com.parkease.vehicle.service;
 
import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.entity.Vehicle;
import com.parkease.vehicle.exception.ResourceNotFoundException;
import com.parkease.vehicle.mapper.VehicleMapper;
import com.parkease.vehicle.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {
 
    private final VehicleRepository repo;
    private final VehicleMapper mapper;
 
    private Vehicle getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }
    
    private void verifyOwner(Vehicle v, String email) {
        if (!v.getOwnerEmail().equals(email)) {
            throw new ResourceNotFoundException("Vehicle not found with id: " + v.getVehicleId());
        }
    }
 
    @Override
    @Transactional
    public VehicleResponseDTO registerVehicle(VehicleRequestDTO dto, String ownerEmail) {
        log.info("Registering vehicle {} for driver: {}", dto.getLicensePlate(), ownerEmail);
 
        String plate = dto.getLicensePlate().toUpperCase().trim();
 
        if (repo.existsByOwnerEmailAndLicensePlate(ownerEmail, plate)) {
            throw new IllegalArgumentException(
                "Vehicle with plate '" + plate + "' is already registered to your account.");
        }
 
        dto.setLicensePlate(plate);
        Vehicle saved = repo.save(mapper.toEntity(dto, ownerEmail));
        log.info("Vehicle registered with id: {}", saved.getVehicleId());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Long vehicleId, VehicleRequestDTO dto, String ownerEmail) {
        log.info("Updating vehicle id: {} for driver: {}", vehicleId, ownerEmail);
        Vehicle v = getOrThrow(vehicleId);
        verifyOwner(v, ownerEmail);
 
        v.setLicensePlate(dto.getLicensePlate().toUpperCase().trim());
        v.setMake(dto.getMake());
        v.setModel(dto.getModel());
        v.setColor(dto.getColor());
        v.setVehicleType(dto.getVehicleType());
        v.setEV(dto.isEV());
 
        return mapper.toDTO(repo.save(v));
    }

    @Override
    @Transactional
    public void deleteVehicle(Long vehicleId, String ownerEmail) {
        log.info("Deleting vehicle id: {} for driver: {}", vehicleId, ownerEmail);
        Vehicle v = getOrThrow(vehicleId);
        verifyOwner(v, ownerEmail);
        repo.delete(v);
    }
 
    @Override
    @Transactional
    public void deactivateVehicle(Long vehicleId, String ownerEmail) {
        log.info("Deactivating vehicle id: {} for driver: {}", vehicleId, ownerEmail);
        Vehicle v = getOrThrow(vehicleId);
        verifyOwner(v, ownerEmail);
        v.setActive(false);
        repo.save(v);
    }
 
    @Override
    public VehicleResponseDTO getVehicleById(Long vehicleId) {
        return mapper.toDTO(getOrThrow(vehicleId));
    }
 
    @Override
    public List<VehicleResponseDTO> getMyVehicles(String ownerEmail) {
        log.debug("Fetching vehicles for: {}", ownerEmail);
        return repo.findByOwnerEmailAndIsActiveTrue(ownerEmail)
                .stream().map(mapper::toDTO).toList();
    }
 
    @Override
    public VehicleResponseDTO getByLicensePlate(String plate) {
        Vehicle v = repo.findByLicensePlate(plate.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with plate: " + plate));
        return mapper.toDTO(v);
    }
}