package com.parkease.payment.service;

import com.parkease.payment.dto.request.CreateOrderRequest;
import com.parkease.payment.dto.request.RefundRequest;
import com.parkease.payment.dto.request.VerifyPaymentRequest;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentStatus;
import com.parkease.payment.exception.PaymentException;
import com.parkease.payment.exception.ResourceNotFoundException;
import com.parkease.payment.mapper.PaymentMapper;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository   repo;
    private final PaymentMapper       mapper;
    private final RazorpayClient      razorpayClient;
    private final ReceiptService      receiptService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private Payment getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequest request, String driverEmail) {
        log.info("Creating Razorpay order for booking: {} amount: ₹{}",
                request.getBookingId(), request.getAmount());

        repo.findByBookingId(request.getBookingId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PAID) {
                throw new PaymentException("Booking " + request.getBookingId()
                        + " is already paid.");
            }
        });

        try {

            int amountInPaise = (int) (request.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",   amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt",  "booking_" + request.getBookingId());
            orderRequest.put("notes", new JSONObject()
                    .put("bookingId",   request.getBookingId())
                    .put("driverEmail", driverEmail));

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            log.info("Razorpay order created: {}", razorpayOrderId);

            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .driverEmail(driverEmail)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status(PaymentStatus.PENDING)
                    .razorpayOrderId(razorpayOrderId)
                    .description(request.getDescription())
                    .build();

            Payment saved = repo.save(payment);

            return OrderResponseDTO.builder()
                    .paymentId(saved.getPaymentId())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status("created")
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponseDTO verifyPayment(VerifyPaymentRequest request) {
        log.info("Verifying payment for order: {}", request.getRazorpayOrderId());

        Payment payment = repo.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + request.getRazorpayOrderId()));

        boolean isValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            log.warn("Invalid signature for order: {}", request.getRazorpayOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            repo.save(payment);
            throw new PaymentException("Payment verification failed. Invalid signature.");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaidAt(LocalDateTime.now());

        try {
            com.razorpay.Payment rzpPayment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
            String method = rzpPayment.get("method");
            payment.setMode(mapPaymentMethod(method));
        } catch (Exception e) {
            log.warn("Could not fetch payment method from Razorpay: {}", e.getMessage());
        }

        Payment saved = repo.save(payment);
        log.info("Payment {} verified and marked as PAID", saved.getPaymentId());

        String receiptPath = receiptService.generateReceipt(saved);
        if (receiptPath != null) {
            saved.setReceiptPath(receiptPath);
            saved = repo.save(saved);
        }

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public PaymentResponseDTO refundPayment(RefundRequest request) {
        log.info("Processing refund for booking: {}", request.getBookingId());

        Payment payment = repo.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for booking: " + request.getBookingId()));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(
                "Cannot refund payment in status: " + payment.getStatus()
                + ". Only PAID payments can be refunded.");
        }

        try {

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("speed", "normal");
            if (request.getReason() != null) {
                refundRequest.put("notes", new JSONObject().put("reason", request.getReason()));
            }

            Refund refund = razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(), refundRequest);

            String refundId = refund.get("id");
            log.info("Razorpay refund created: {}", refundId);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refundId);
            payment.setRefundedAt(LocalDateTime.now());

            return mapper.toDTO(repo.save(payment));

        } catch (RazorpayException e) {
            log.error("Refund failed for payment {}: {}", payment.getPaymentId(), e.getMessage());
            throw new PaymentException("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDTO getPaymentByBookingId(Long bookingId) {
        return mapper.toDTO(
            repo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment not found for booking: " + bookingId))
        );
    }

    @Override
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        return mapper.toDTO(getOrThrow(paymentId));
    }

    @Override
    public List<PaymentResponseDTO> getMyPayments(String driverEmail) {
        return repo.findByDriverEmailOrderByCreatedAtDesc(driverEmail)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<PaymentResponseDTO> getAllPayments() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public String getReceiptPath(Long paymentId, String driverEmail) {
        Payment payment = getOrThrow(paymentId);

        if (!payment.getDriverEmail().equals(driverEmail)) {
            throw new ResourceNotFoundException("Payment not found with id: " + paymentId);
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException("Receipt is only available for completed payments.");
        }

        if (payment.getReceiptPath() == null ||
                !new java.io.File(payment.getReceiptPath()).exists()) {
            log.info("Receipt missing for payment {}, regenerating...", paymentId);
            String path = receiptService.generateReceipt(payment);
            payment.setReceiptPath(path);
            repo.save(payment);
        }

        return payment.getReceiptPath();
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private com.parkease.payment.entity.PaymentMode mapPaymentMethod(String method) {
        if (method == null) return null;
        return switch (method.toLowerCase()) {
            case "card"        -> com.parkease.payment.entity.PaymentMode.CARD;
            case "upi"         -> com.parkease.payment.entity.PaymentMode.UPI;
            case "netbanking"  -> com.parkease.payment.entity.PaymentMode.NETBANKING;
            default            -> null;
        };
    }
}
