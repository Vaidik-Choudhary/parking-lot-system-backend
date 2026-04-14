package com.parkease.payment.service;

import com.parkease.payment.dto.request.CreateOrderRequest;
import com.parkease.payment.dto.request.RefundRequest;
import com.parkease.payment.dto.request.VerifyPaymentRequest;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;

import java.util.List;


public interface PaymentService {

    OrderResponseDTO createOrder(CreateOrderRequest request, String driverEmail);

    PaymentResponseDTO verifyPayment(VerifyPaymentRequest request);

    PaymentResponseDTO refundPayment(RefundRequest request);

    PaymentResponseDTO getPaymentByBookingId(Long bookingId);
    PaymentResponseDTO getPaymentById(Long paymentId);
    List<PaymentResponseDTO> getMyPayments(String driverEmail);
    List<PaymentResponseDTO> getAllPayments(); 

    String getReceiptPath(Long paymentId, String driverEmail);
}
