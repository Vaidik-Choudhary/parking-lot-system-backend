package com.parkease.booking.repository;

import com.parkease.booking.entity.SubscriptionRequest;
import com.parkease.booking.entity.SubscriptionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRequestRepository extends JpaRepository<SubscriptionRequest, Long> {
    List<SubscriptionRequest> findByLotIdAndStatus(Long lotId, SubscriptionRequestStatus status);
    List<SubscriptionRequest> findByDriverEmailOrderByCreatedAtDesc(String driverEmail);
}
