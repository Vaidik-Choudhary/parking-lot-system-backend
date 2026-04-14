package com.parkease.payment.mapper;

import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toDTO(Payment p) {
        return PaymentResponseDTO.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .driverEmail(p.getDriverEmail())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .mode(p.getMode())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .razorpayRefundId(p.getRazorpayRefundId())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .build();
    }
}
