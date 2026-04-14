package com.parkease.parkingspot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkease.parkingspot.entity.ParkingSpot;
import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;

@Repository
public interface SpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByLotId(Long lotId);

    List<ParkingSpot> findByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotType(Long lotId, SpotType spotType);

    List<ParkingSpot> findByLotIdAndVehicleType(Long lotId, VehicleType vehicleType);

    List<ParkingSpot> findByLotIdAndIsEVChargingTrue(Long lotId);

    List<ParkingSpot> findByLotIdAndIsHandicappedTrue(Long lotId);

    List<ParkingSpot> findByLotIdAndStatusAndSpotType(Long lotId, SpotStatus status, SpotType spotType);

    List<ParkingSpot> findByLotIdAndStatusAndVehicleType(Long lotId, SpotStatus status, VehicleType vehicleType);

    int countByLotIdAndStatus(Long lotId, SpotStatus status);

    boolean existsByLotIdAndSpotNumber(Long lotId, String spotNumber);

    List<ParkingSpot> findByLotIdAndFloor(Long lotId, int floor);

    Optional<ParkingSpot> findByLotIdAndSpotNumber(Long lotId, String spotNumber);
}
