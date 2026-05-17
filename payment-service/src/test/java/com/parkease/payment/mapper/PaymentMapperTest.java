package com.parkease.payment.mapper;

import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {
    private PaymentMapper mapper;
    @BeforeEach void setUp() { mapper = new PaymentMapper(); }

    @Test void toDTO_paidPayment_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Payment p = Payment.builder().paymentId(1L).bookingId(10L).driverEmail("d@t.com")
                .amount(200.0).currency("INR").status(PaymentStatus.PAID).mode(PaymentMode.UPI)
                .razorpayOrderId("order_123").razorpayPaymentId("pay_123")
                .description("Parking").paidAt(now).build();
        PaymentResponseDTO dto = mapper.toDTO(p);
        assertEquals(1L, dto.getPaymentId());
        assertEquals(10L, dto.getBookingId());
        assertEquals(200.0, dto.getAmount());
        assertEquals(PaymentStatus.PAID, dto.getStatus());
        assertEquals(PaymentMode.UPI, dto.getMode());
        assertEquals("pay_123", dto.getRazorpayPaymentId());
        assertEquals(now, dto.getPaidAt());
    }

    @Test void toDTO_refundedPayment_mapsRefundFields() {
        LocalDateTime refundedAt = LocalDateTime.now();
        Payment p = Payment.builder().paymentId(2L).bookingId(20L).driverEmail("d@t.com")
                .amount(150.0).currency("INR").status(PaymentStatus.REFUNDED)
                .razorpayOrderId("order_456").razorpayPaymentId("pay_456")
                .razorpayRefundId("refund_789").refundedAt(refundedAt).build();
        PaymentResponseDTO dto = mapper.toDTO(p);
        assertEquals(PaymentStatus.REFUNDED, dto.getStatus());
        assertEquals("refund_789", dto.getRazorpayRefundId());
        assertEquals(refundedAt, dto.getRefundedAt());
    }

    @Test void toDTO_pendingPayment_nullPaidAt() {
        Payment p = Payment.builder().paymentId(3L).bookingId(30L).driverEmail("d@t.com")
                .amount(100.0).currency("INR").status(PaymentStatus.PENDING)
                .razorpayOrderId("order_789").build();
        PaymentResponseDTO dto = mapper.toDTO(p);
        assertEquals(PaymentStatus.PENDING, dto.getStatus());
        assertNull(dto.getPaidAt());
        assertNull(dto.getRazorpayPaymentId());
    }
}
