package com.parkease.payment.controller;

import com.parkease.payment.dto.request.CreateOrderRequest;
import com.parkease.payment.dto.request.RefundRequest;
import com.parkease.payment.dto.request.VerifyPaymentRequest;
import com.parkease.payment.dto.response.ApiResponse;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/order")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("POST /api/payments/order - driver: {} booking: {}", email, request.getBookingId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createOrder(request, email));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDTO> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        log.info("POST /api/payments/verify - order: {}", request.getRazorpayOrderId());
        return ResponseEntity.ok(service.verifyPayment(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponseDTO> refund(
            @Valid @RequestBody RefundRequest request) {
        log.info("POST /api/payments/refund - booking: {}", request.getBookingId());
        return ResponseEntity.ok(service.refundPayment(request));
    }


    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("GET /api/payments/my - driver: {}", email);
        return ResponseEntity.ok(service.getMyPayments(email));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponseDTO> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getPaymentByBookingId(bookingId));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(service.getPaymentById(paymentId));
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<Resource> downloadReceipt(
            @PathVariable Long paymentId,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("GET /api/payments/{}/receipt - driver: {}", paymentId, email);

        String filePath = service.getReceiptPath(paymentId, email);
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=receipt_" + paymentId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        log.info("GET /api/payments/admin/all");
        return ResponseEntity.ok(service.getAllPayments());
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.info("Webhook received from Razorpay");
        return ResponseEntity.ok(ApiResponse.ok("Webhook received."));
    }
}
