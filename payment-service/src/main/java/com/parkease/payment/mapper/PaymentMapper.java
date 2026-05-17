package com.parkease.payment.mapper;

import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toDTO(Payment p) {
        if (p == null) return null;
        
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setPaymentId(p.getPaymentId());
        dto.setBookingId(p.getBookingId());
        dto.setSubscriptionId(p.getSubscriptionId());
        dto.setDriverEmail(p.getDriverEmail());
        dto.setAmount(p.getAmount());
        dto.setCurrency(p.getCurrency());
        dto.setStatus(p.getStatus());
        dto.setMode(p.getMode());
        dto.setRazorpayOrderId(p.getRazorpayOrderId());
        dto.setRazorpayPaymentId(p.getRazorpayPaymentId());
        dto.setRazorpayRefundId(p.getRazorpayRefundId());
        dto.setDescription(p.getDescription());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setPaidAt(p.getPaidAt());
        dto.setRefundedAt(p.getRefundedAt());
        
        return dto;
    }
}
