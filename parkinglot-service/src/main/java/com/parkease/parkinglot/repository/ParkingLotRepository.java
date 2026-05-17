package com.parkease.parkinglot.repository;

import com.parkease.parkinglot.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    
    List<ParkingLot> findByManagerEmail(String managerEmail);
    List<ParkingLot> findByIsOpenTrueAndIsApprovedTrue();
    List<ParkingLot> findByIsApprovedFalse();

    @Query(value = """
        SELECT * FROM parking_lots
        WHERE is_approved = true
          AND LOWER(city) = LOWER(:city)
          AND hasev = COALESCE(:hasEV, IFNULL(hasev, 0))
          AND has_two_wheeler = COALESCE(:has2W, IFNULL(has_two_wheeler, 0))
          AND has_four_wheeler = COALESCE(:has4W, IFNULL(has_four_wheeler, 0))
          AND has_heavy = COALESCE(:hasHeavy, IFNULL(has_heavy, 0))
          AND is_handicapped_friendly = COALESCE(:hasHandicap, IFNULL(is_handicapped_friendly, 0))
        """, nativeQuery = true)
    List<ParkingLot> findByCityWithFilters(
            @Param("city") String city,
            @Param("hasEV") Boolean hasEV,
            @Param("has2W") Boolean has2W,
            @Param("has4W") Boolean has4W,
            @Param("hasHeavy") Boolean hasHeavy,
            @Param("hasHandicap") Boolean hasHandicap
    );

    @Query(value = """
        SELECT *, (
            6371 * ACOS(
                LEAST(1.0, GREATEST(-1.0, 
                    COS(RADIANS(:#{#req.lat})) * COS(RADIANS(latitude))
                    * COS(RADIANS(longitude) - RADIANS(:#{#req.lon}))
                    + SIN(RADIANS(:#{#req.lat})) * SIN(RADIANS(latitude))
                ))
            )
        ) AS distance
        FROM parking_lots
        WHERE is_approved = true
          AND is_open = true
          AND hasev = COALESCE(:#{#req.hasEV}, IFNULL(hasev, 0))
          AND has_two_wheeler = COALESCE(:#{#req.has2W}, IFNULL(has_two_wheeler, 0))
          AND has_four_wheeler = COALESCE(:#{#req.has4W}, IFNULL(has_four_wheeler, 0))
          AND has_heavy = COALESCE(:#{#req.hasHeavy}, IFNULL(has_heavy, 0))
          AND is_handicapped_friendly = COALESCE(:#{#req.hasHandicap}, IFNULL(is_handicapped_friendly, 0))
        HAVING distance <= :#{#req.radiusKm}
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<ParkingLot> findNearbyWithFilters(@Param("req") com.parkease.parkinglot.dto.request.NearbySearchRequest req);
}
