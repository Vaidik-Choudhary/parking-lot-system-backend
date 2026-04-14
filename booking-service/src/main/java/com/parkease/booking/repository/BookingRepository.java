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

    List<Booking> findByDriverEmailOrderByCreatedAtDesc(String driverEmail);

    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);

    List<Booking> findBySpotId(Long spotId);

    Optional<Booking> findBySpotIdAndStatus(Long spotId, BookingStatus status);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByDriverEmailAndStatus(String driverEmail, BookingStatus status);

    List<Booking> findByLotIdAndStatus(Long lotId, BookingStatus status);
    
    int countByLotIdAndStatus(Long lotId, BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'RESERVED'
          AND b.bookingType = 'PRE_BOOKING'
          AND b.startTime < :cutoffTime
        """)
    List<Booking> findExpiredPreBookings(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.spotId = :spotId
          AND b.status IN ('RESERVED', 'ACTIVE')
          AND b.startTime < :endTime
          AND b.endTime > :startTime
        """)
    boolean isSpotBookedInWindow(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
