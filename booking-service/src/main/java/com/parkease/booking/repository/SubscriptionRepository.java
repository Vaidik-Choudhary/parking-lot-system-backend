package com.parkease.booking.repository;

import com.parkease.booking.entity.Subscription;
import com.parkease.booking.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByDriverEmailOrderByStartDateDesc(String driverEmail);
    List<Subscription> findByLotIdAndStatus(Long lotId, SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.spotId = :spotId AND s.status IN ('ACTIVE', 'SCHEDULED') AND s.startDate < :endTime AND s.endDate > :startTime")
    List<Subscription> findOverlappingSubscriptions(@Param("spotId") Long spotId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Optional<Subscription> findBySpotIdAndStatus(Long spotId, SubscriptionStatus status);

    List<Subscription> findByStatusAndStartDateLessThanEqual(SubscriptionStatus status, LocalDateTime now);
    List<Subscription> findByStatusAndEndDateLessThanEqual(SubscriptionStatus status, LocalDateTime now);
}
