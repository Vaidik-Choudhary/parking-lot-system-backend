package com.parkease.payment.service;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @InjectMocks
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(receiptService, "storagePath", "target/receipts/");
    }

    @Test
    void shouldGenerateReceiptSuccessfully() {
        Payment payment = Payment.builder()
                .paymentId(123L)
                .bookingId(456L)
                .driverEmail("test@example.com")
                .amount(150.50)
                .currency("INR")
                .mode(PaymentMode.CARD)
                .razorpayPaymentId("rzp_pay_123")
                .razorpayOrderId("rzp_order_123")
                .paidAt(LocalDateTime.now())
                .description("Test Parking Payment")
                .build();

        String receiptPath = receiptService.generateReceipt(payment);

        assertNotNull(receiptPath);
        File receiptFile = new File(receiptPath);
        assertTrue(receiptFile.exists());
        assertTrue(receiptFile.length() > 0);
    }
    @Test
    void shouldGenerateReceiptForSubscriptionSuccessfully() {
        Payment payment = Payment.builder()
                .paymentId(124L)
                .subscriptionId(789L)
                .driverEmail("test2@example.com")
                .amount(500.00)
                .currency("INR")
                .paidAt(LocalDateTime.now())
                .build();

        String receiptPath = receiptService.generateReceipt(payment);

        assertNotNull(receiptPath);
        File receiptFile = new File(receiptPath);
        assertTrue(receiptFile.exists());
    }

    @Test
    void shouldHandleExceptionWhenGeneratingReceipt() {
        ReflectionTestUtils.setField(receiptService, "storagePath", "target/receipts/");
        Payment payment = mock(Payment.class);
        when(payment.getPaymentId()).thenReturn(125L);
        when(payment.getAmount()).thenThrow(new RuntimeException("Test Error"));
        
        String result = receiptService.generateReceipt(payment);
        
        org.junit.jupiter.api.Assertions.assertNull(result);
    }
}
