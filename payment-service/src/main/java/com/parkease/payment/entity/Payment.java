package com.parkease.payment.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Long bookingId;
    private Long subscriptionId;
    
    @Column(nullable = false)
    private String driverEmail;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    @Builder.Default
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
