package com.parkease.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true)
    private Long bookingId;
    
    @Column(nullable = false)
    private String driverEmail;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMode mode;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

    private String razorpayRefundId;

    private String receiptPath;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (currency == null) currency = "INR";
    }
}
