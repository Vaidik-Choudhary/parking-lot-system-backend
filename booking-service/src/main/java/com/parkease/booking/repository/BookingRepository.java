package com.parkease.booking.repository;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // â”€â”€ Basic finders â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    List<Booking> findByDriverEmailOrderByCreatedAtDesc(String driverEmail);

    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);

    List<Booking> findBySpotId(Long spotId);

    boolean existsBySpotIdAndStatus(Long spotId, BookingStatus status);

    boolean existsByVehiclePlateAndStatus(String vehiclePlate, BookingStatus status);

    Optional<Booking> findBySpotIdAndStatus(Long spotId, BookingStatus status);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByDriverEmailAndStatus(String driverEmail, BookingStatus status);

    List<Booking> findByLotIdAndStatus(Long lotId, BookingStatus status);

    int countByLotIdAndStatus(Long lotId, BookingStatus status);

    // â”€â”€ Scheduler: auto-cancel expired PRE_BOOKINGs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Finds PRE_BOOKING bookings still in RESERVED status whose startTime is
     * before the cutoff (startTime + graceMinutes has already passed).
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'RESERVED'
          AND b.bookingType = 'PRE_BOOKING'
          AND b.startTime < :cutoffTime
        """)
    List<Booking> findExpiredPreBookings(@Param("cutoffTime") LocalDateTime cutoffTime);

    // â”€â”€ Double-booking guard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns true if the given spot already has a RESERVED or ACTIVE booking
     * whose time window overlaps [startTime, endTime].
     *
     * Overlap condition (BookMyShow-style):
     *   existing.startTime < requested.endTime  AND
     *   existing.endTime   > requested.startTime
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.spotId = :spotId
          AND b.status IN ('RESERVED', 'ACTIVE')
          AND b.startTime < :endTime
          AND b.endTime   > :startTime
        """)
    boolean isSpotBookedInWindow(
            @Param("spotId")    Long          spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime")   LocalDateTime endTime
    );

    /**
     * Returns true if the given vehicle already has a RESERVED or ACTIVE booking
     * whose time window overlaps [startTime, endTime].
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.vehiclePlate = :vehiclePlate
          AND b.status IN ('RESERVED', 'ACTIVE')
          AND b.startTime < :endTime
          AND b.endTime   > :startTime
        """)
    boolean isVehicleBookedInWindow(
            @Param("vehiclePlate") String        vehiclePlate,
            @Param("startTime")    LocalDateTime startTime,
            @Param("endTime")      LocalDateTime endTime
    );

    // â”€â”€ Pre-booking: time-filtered slot discovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns the IDs of spots in the given lot that are already booked
     * (RESERVED or ACTIVE) within the requested [startTime, endTime] window.
     * The frontend uses this to grey-out unavailable slots.
     */
    @Query("""
        SELECT DISTINCT b.spotId FROM Booking b
        WHERE b.lotId     = :lotId
          AND b.status    IN ('RESERVED', 'ACTIVE')
          AND b.startTime < :endTime
          AND b.endTime   > :startTime
        """)
    List<Long> findBookedSpotIdsInWindow(
            @Param("lotId")     Long          lotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime")   LocalDateTime endTime
    );

    // â”€â”€ Drive-in: per-spot live reservation lookup â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns all RESERVED or ACTIVE bookings for a specific spot, ordered by
     * startTime. Used by the drive-in view to find the nearest upcoming
     * reservation and label the spot "Available until HH:mm".
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.spotId = :spotId
          AND b.status IN ('RESERVED', 'ACTIVE')
        ORDER BY b.startTime ASC
        """)
    List<Booking> findActiveOrReservedBookingsForSpot(@Param("spotId") Long spotId);

    // â”€â”€ Manager dashboard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Active bookings for a lot: status = ACTIVE (driver has checked in).
     * currentTime is implicitly "now" â€” any ACTIVE booking is by definition
     * currently in progress.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.lotId  = :lotId
          AND b.status = 'ACTIVE'
        ORDER BY b.startTime ASC
        """)
    List<Booking> findActiveBookingsByLot(@Param("lotId") Long lotId);

    /**
     * Upcoming bookings for a lot: status = RESERVED, startTime > now.
     * Shows the manager who is expected to arrive next.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.lotId     = :lotId
          AND b.status    = 'RESERVED'
          AND b.startTime > :now
        ORDER BY b.startTime ASC
        """)
    List<Booking> findUpcomingBookingsByLot(
            @Param("lotId") Long          lotId,
            @Param("now")   LocalDateTime now
    );

    // â”€â”€ Analytics helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Query("SELECT DISTINCT b.lotId FROM Booking b")
    List<Long> findDistinctLotIds();
}
