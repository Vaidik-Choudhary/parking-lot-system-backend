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
import com.parkease.payment.messaging.NotificationEvent;
import com.parkease.payment.messaging.NotificationPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Order;
import com.razorpay.Refund;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
public class PaymentServiceImpl implements PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository    repo;
    private final PaymentMapper        mapper;
    private final RazorpayClient       razorpayClient;
    private final ReceiptService       receiptService;
    private final NotificationPublisher notificationPublisher;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private com.parkease.payment.entity.Payment getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequest request, String driverEmail) {
        Long bookingId = request.getBookingId();
        Long subId = request.getSubscriptionId();
        
        log.info("Creating Razorpay order - Booking: {}, Subscription: {}, Amount: Rs.{}",
                bookingId, subId, request.getAmount());

        // Check if already paid
        if (bookingId != null) {
            repo.findByBookingId(bookingId).ifPresent(p -> {
                if (p.getStatus() == PaymentStatus.PAID) throw new PaymentException("Booking is already paid.");
            });
        } else if (subId != null) {
            repo.findBySubscriptionId(subId).ifPresent(p -> {
                if (p.getStatus() == PaymentStatus.PAID) throw new PaymentException("Subscription is already paid.");
            });
        } else {
            throw new PaymentException("Either Booking ID or Subscription ID is required.");
        }

        try {
            int amountInPaise = (int) (request.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",   amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt",  bookingId != null ? "booking_" + bookingId : "sub_" + subId);
            
            JSONObject notes = new JSONObject();
            notes.put("driverEmail", driverEmail);
            if (bookingId != null) notes.put("bookingId", bookingId);
            if (subId != null) notes.put("subscriptionId", subId);
            
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            log.info("Razorpay order created: {}", razorpayOrderId);

            // Reuse existing PENDING payment or create new
            com.parkease.payment.entity.Payment payment;
            if (bookingId != null) {
                payment = repo.findByBookingId(bookingId)
                        .filter(p -> p.getStatus() == com.parkease.payment.entity.PaymentStatus.PENDING)
                        .orElseGet(() -> com.parkease.payment.entity.Payment.builder().bookingId(bookingId).status(com.parkease.payment.entity.PaymentStatus.PENDING).build());
            } else {
                payment = repo.findBySubscriptionId(subId)
                        .filter(p -> p.getStatus() == com.parkease.payment.entity.PaymentStatus.PENDING)
                        .orElseGet(() -> com.parkease.payment.entity.Payment.builder().subscriptionId(subId).status(com.parkease.payment.entity.PaymentStatus.PENDING).build());
            }
            
            payment.setDriverEmail(driverEmail);
            payment.setCurrency("INR");

            payment.setAmount(request.getAmount());
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setDescription(request.getDescription());

            com.parkease.payment.entity.Payment saved = repo.save(payment);

            return OrderResponseDTO.builder()
                    .paymentId(saved.getPaymentId())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status("created")
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            throw new PaymentException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void initializePayment(CreateOrderRequest request, String driverEmail) {
        log.info("Initializing PENDING payment record for booking: {} amount: Rs.{}",
                request.getBookingId(), request.getAmount());

        repo.findByBookingId(request.getBookingId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PAID) {
                throw new PaymentException("Booking " + request.getBookingId() + " is already paid.");
            }
            // If already exists and is PENDING, we just update the amount/description if needed
            existing.setAmount(request.getAmount());
            existing.setDescription(request.getDescription());
            repo.save(existing);
        });

        if (repo.findByBookingId(request.getBookingId()).isEmpty()) {
            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .driverEmail(driverEmail)
                    .amount(request.getAmount())
                    .currency("INR")
                    .status(PaymentStatus.PENDING)
                    .description(request.getDescription())
                    .build();
            repo.save(payment);
            log.info("Created initial PENDING record for booking {}", request.getBookingId());
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

        // Publish payment success notification
        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(saved.getDriverEmail())
                .type("PAYMENT")
                .title("Payment Successful!")
                .message("Your payment of Rs." + saved.getAmount()
                        + " for booking #" + saved.getBookingId()
                        + " was successful. Payment ID: " + saved.getRazorpayPaymentId()
                        + ". Your receipt is available in the app.")
                .relatedId(saved.getPaymentId())
                .relatedType("PAYMENT")
                .build());

        return mapper.toDTO(saved);
    }


    @Override
    @Transactional
    public PaymentResponseDTO refundPayment(RefundRequest request) {
        com.parkease.payment.entity.Payment payment = repo.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + request.getBookingId()));

        if (payment.getStatus() != com.parkease.payment.entity.PaymentStatus.PAID) {
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

            payment.setStatus(com.parkease.payment.entity.PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refundId);
            payment.setRefundedAt(LocalDateTime.now());

            com.parkease.payment.entity.Payment saved = repo.save(payment);

            // Publish refund notification
            notificationPublisher.publish(NotificationEvent.builder()
                    .recipientEmail(saved.getDriverEmail())
                    .type("PAYMENT")
                    .title("Refund Initiated")
                    .message("A refund of Rs." + saved.getAmount()
                            + " for booking #" + saved.getBookingId()
                            + " has been initiated. Refund ID: " + refundId
                            + ". It will reflect in 5-7 business days.")
                    .relatedId(saved.getPaymentId())
                    .relatedType("PAYMENT")
                    .build());

            return mapper.toDTO(saved);

        } catch (RazorpayException e) {
            throw new PaymentException("Refund failed for payment " + payment.getPaymentId() + ": " + e.getMessage(), e);
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
        String normalizedEmail = driverEmail == null ? "" : driverEmail.trim().toLowerCase();
        log.info("getMyPayments query for email: '{}'", normalizedEmail);
        List<com.parkease.payment.entity.Payment> results = repo.findByDriverEmailOrderByCreatedAtDesc(normalizedEmail);
        log.info("DB returned {} payment records for '{}'", results.size(), normalizedEmail);
        return results.stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<PaymentResponseDTO> getAllPayments() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public long getPendingPaymentsCount(String driverEmail) {
        return repo.countByDriverEmailAndStatusIn(driverEmail, List.of(PaymentStatus.PENDING, PaymentStatus.FAILED));
    }

    @Override
    @Transactional
    public void initializeSubscriptionPayment(com.parkease.payment.dto.request.SubscriptionPaymentRequest request) {
        log.info("=== initializeSubscriptionPayment called === Sub: {}, Email: {}, Amount: {}",
                request.getSubscriptionId(), request.getDriverEmail(), request.getAmount());

        if (request.getDriverEmail() == null || request.getDriverEmail().isBlank()) {
            log.error("Cannot create subscription payment - driverEmail is null/blank for sub #{}", request.getSubscriptionId());
            return;
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            log.error("Cannot create subscription payment - amount is invalid ({}) for sub #{}", request.getAmount(), request.getSubscriptionId());
            return;
        }

        // Always create a fresh PENDING payment for each cancellation
        com.parkease.payment.entity.Payment payment = new com.parkease.payment.entity.Payment();
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setDriverEmail(request.getDriverEmail().trim().toLowerCase());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setStatus(com.parkease.payment.entity.PaymentStatus.PENDING);
        payment.setDescription(request.getDescription());

        com.parkease.payment.entity.Payment saved = repo.save(payment);
        log.info("=== Subscription payment record saved === paymentId: {}, sub: {}, email: {}, amount: {}",
                saved.getPaymentId(), saved.getSubscriptionId(), saved.getDriverEmail(), saved.getAmount());
    }

    @Override
    public String getReceiptPath(Long paymentId, String driverEmail) {
        com.parkease.payment.entity.Payment payment = getOrThrow(paymentId);

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

    // â”€â”€ private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            case "card"       -> com.parkease.payment.entity.PaymentMode.CARD;
            case "upi"        -> com.parkease.payment.entity.PaymentMode.UPI;
            case "netbanking" -> com.parkease.payment.entity.PaymentMode.NETBANKING;
            default           -> null;
        };
    }
}
