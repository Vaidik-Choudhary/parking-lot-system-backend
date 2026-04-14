package com.parkease.parkinglot.repository;

import com.parkease.parkinglot.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    List<ParkingLot> findByCityIgnoreCaseAndIsApprovedTrue(String city);

    List<ParkingLot> findByManagerEmail(String managerEmail);

    List<ParkingLot> findByAvailableSpotsGreaterThanAndIsApprovedTrue(int count);

    List<ParkingLot> findByIsOpenTrueAndIsApprovedTrue();

    List<ParkingLot> findByIsApprovedFalse();


    @Query(value = """
        SELECT *, (
            6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                * COS(RADIANS(longitude) - RADIANS(:lon))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
            )
        ) AS distance
        FROM parking_lots
        WHERE is_approved = true
          AND is_open = true
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<ParkingLot> findNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm
    );
}
