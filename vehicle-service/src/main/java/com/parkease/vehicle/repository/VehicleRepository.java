package com.parkease.vehicle.repository;
 
import com.parkease.vehicle.entity.Vehicle;
import com.parkease.vehicle.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
 
    List<Vehicle> findByOwnerEmail(String ownerEmail);
 
    List<Vehicle> findByOwnerEmailAndIsActiveTrue(String ownerEmail);
 
    Optional<Vehicle> findByOwnerEmailAndLicensePlate(String ownerEmail, String licensePlate);
 
    Optional<Vehicle> findByLicensePlate(String licensePlate);
 
    boolean existsByOwnerEmailAndLicensePlate(String ownerEmail, String licensePlate);
 
    List<Vehicle> findByVehicleType(VehicleType vehicleType);
 
    List<Vehicle> findByIsEVTrue();
}
 
