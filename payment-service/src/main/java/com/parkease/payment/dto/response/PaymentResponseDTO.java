package com.parkease.payment.dto.response;

import com.parkease.payment.entity.PaymentMode;
import com.parkease.payment.entity.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class PaymentResponseDTO {

    private Long paymentId;
    private Long bookingId;
    private String driverEmail;
    private double amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMode mode;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpayRefundId;

    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}
