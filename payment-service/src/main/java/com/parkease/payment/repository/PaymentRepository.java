package com.parkease.payment.repository;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByDriverEmailOrderByCreatedAtDesc(String driverEmail);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID' " +
           "AND p.paidAt BETWEEN :from AND :to")
    Double getTotalRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Payment> findByStatusAndPaidAtBetween(
            PaymentStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
}
