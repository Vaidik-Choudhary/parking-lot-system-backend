package com.parkease.payment.service;

import com.parkease.payment.dto.request.CreateOrderRequest;
import com.parkease.payment.dto.request.RefundRequest;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentStatus;
import com.parkease.payment.exception.PaymentException;
import com.parkease.payment.mapper.PaymentMapper;
import com.parkease.payment.messaging.NotificationPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repo;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private PaymentServiceImpl service;

    private Payment payment;
    private PaymentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "test_secret");

        // IMPORTANT: mock nested Razorpay SDK clients
        razorpayClient.orders = orderClient;
        razorpayClient.payments = paymentClient;

        payment = Payment.builder()
                .paymentId(1L)
                .bookingId(100L)
                .driverEmail("vaidik@test.com")
                .amount(150.0)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .build();

        responseDTO = new PaymentResponseDTO();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);
        req.setDescription("Parking payment");

        Order mockOrder = mock(Order.class);

        when(repo.findByBookingId(100L)).thenReturn(Optional.empty());
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(repo.save(any(Payment.class))).thenReturn(payment);

        OrderResponseDTO result = service.createOrder(req, "vaidik@test.com");

        assertNotNull(result);
        assertEquals("order_123", result.getRazorpayOrderId());
        verify(repo).save(any(Payment.class));
    }

    @Test
    void shouldThrowWhenPaymentAlreadyPaid() {
        payment.setStatus(PaymentStatus.PAID);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);

        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> service.createOrder(req, "vaidik@test.com"));
    }

    @Test
    void shouldRefundPaymentSuccessfully() throws Exception {
        payment.setStatus(PaymentStatus.PAID);

        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);
        req.setReason("User cancelled");

        Refund mockRefund = mock(Refund.class);

        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        when(paymentClient.refund(eq("pay_123"), any(JSONObject.class)))
                .thenReturn(mockRefund);
        when(mockRefund.get("id")).thenReturn("refund_123");
        when(repo.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDTO(any(Payment.class))).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        PaymentResponseDTO result = service.refundPayment(req);

        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(repo).save(payment);
    }

    @Test
    void shouldThrowWhenRefundingNonPaidPayment() {
        payment.setStatus(PaymentStatus.PENDING);

        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> service.refundPayment(req));
    }

    @Test
    void shouldRegenerateReceiptWhenMissing() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptPath(null);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(receiptService.generateReceipt(payment))
                .thenReturn("C:/temp/new_receipt.pdf");

        String result = service.getReceiptPath(1L, "vaidik@test.com");

        assertEquals("C:/temp/new_receipt.pdf", result);
        verify(repo).save(payment);
    }

    @Test
    void shouldGetReceiptPathSuccessfully() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptPath(null);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(receiptService.generateReceipt(payment))
                .thenReturn("C:/temp/receipt.pdf");

        String result = service.getReceiptPath(1L, "vaidik@test.com");

        assertEquals("C:/temp/receipt.pdf", result);
    }

    @Test
    void shouldThrowWhenReceiptRequestedByAnotherDriver() {
        payment.setStatus(PaymentStatus.PAID);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class,
                () -> service.getReceiptPath(1L, "other@test.com"));
    }

    @Test
    void getMyPayments_returnsList() {
        when(repo.findByDriverEmailOrderByCreatedAtDesc("vaidik@test.com")).thenReturn(java.util.List.of(payment));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getMyPayments("vaidik@test.com").size());
    }

    @Test
    void getPaymentByBookingId_Success() {
        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertNotNull(service.getPaymentByBookingId(100L));
    }

    @Test
    void getPendingCount_Success() {
        when(repo.countByDriverEmailAndStatusIn("vaidik@test.com", List.of(PaymentStatus.PENDING, PaymentStatus.FAILED))).thenReturn(2L);
        assertEquals(2L, service.getPendingPaymentsCount("vaidik@test.com"));
    }

    @Test
    void initializePayment_Success() {
        com.parkease.payment.dto.request.CreateOrderRequest req = new com.parkease.payment.dto.request.CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);
        
        when(repo.findByBookingId(100L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(payment);
        
        service.initializePayment(req, "vaidik@test.com");
        verify(repo).save(any());
    }

    @Test
    void createOrder_ThrowsRazorpayException() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L); req.setAmount(150.0);
        when(repo.findByBookingId(100L)).thenReturn(Optional.empty());
        when(orderClient.create(any(JSONObject.class))).thenThrow(new com.razorpay.RazorpayException("Test Razorpay Error"));
        assertThrows(PaymentException.class, () -> service.createOrder(req, "v@t.com"));
    }

    @Test
    void initializePayment_AlreadyPaid_Throws() {
        payment.setStatus(PaymentStatus.PAID);
        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        assertThrows(PaymentException.class, () -> service.initializePayment(req, "v@t.com"));
    }

    @Test
    void verifyPayment_InvalidSignature_Throws() {
        com.parkease.payment.dto.request.VerifyPaymentRequest req = new com.parkease.payment.dto.request.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("invalid_sig");
        when(repo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        
        // Let verifySignature return false because we gave dummy secret/sig
        assertThrows(PaymentException.class, () -> service.verifyPayment(req));
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(repo).save(payment);
    }

    @Test
    void refundPayment_ThrowsRazorpayException() throws Exception {
        payment.setStatus(PaymentStatus.PAID);
        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);
        when(paymentClient.refund(eq("pay_123"), any(JSONObject.class)))
            .thenThrow(new com.razorpay.RazorpayException("Test Refund Error"));
        assertThrows(PaymentException.class, () -> service.refundPayment(req));
    }

    @Test
    void getReceiptPath_NotPaid_Throws() {
        payment.setStatus(PaymentStatus.PENDING);
        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        assertThrows(PaymentException.class, () -> service.getReceiptPath(1L, "vaidik@test.com"));
    }

    @Test
    void verifyPayment_Success() throws Exception {
        com.parkease.payment.dto.request.VerifyPaymentRequest req = new com.parkease.payment.dto.request.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        
        String secret = "test_secret";
        String payload = "order_123|pay_123";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        String signature = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
        req.setRazorpaySignature(signature);

        com.razorpay.Payment rzpPayment = mock(com.razorpay.Payment.class);
        when(repo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(paymentClient.fetch("pay_123")).thenReturn(rzpPayment);
        when(rzpPayment.get("method")).thenReturn("upi");
        when(repo.save(any())).thenReturn(payment);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        PaymentResponseDTO result = service.verifyPayment(req);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(com.parkease.payment.entity.PaymentMode.UPI, payment.getMode());
        verify(notificationPublisher).publish(any());
    }

    @Test
    void getAllPayments_returnsList() {
        when(repo.findAll()).thenReturn(List.of(payment));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getAllPayments().size());
    }

    @Test
    void getPaymentById_returnsPayment() {
        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertNotNull(service.getPaymentById(1L));
    }

    @Test
    void initializeSubscriptionPayment_Success() {
        com.parkease.payment.dto.request.SubscriptionPaymentRequest req = new com.parkease.payment.dto.request.SubscriptionPaymentRequest();
        req.setDriverEmail("v@t.com");
        req.setSubscriptionId(10L);
        req.setAmount(100.0);
        when(repo.save(any())).thenReturn(payment);
        service.initializeSubscriptionPayment(req);
        verify(repo).save(any());
    }

    @Test
    void initializeSubscriptionPayment_InvalidEmail() {
        com.parkease.payment.dto.request.SubscriptionPaymentRequest req = new com.parkease.payment.dto.request.SubscriptionPaymentRequest();
        req.setDriverEmail("");
        service.initializeSubscriptionPayment(req);
        verify(repo, never()).save(any());
    }

    @Test
    void initializeSubscriptionPayment_InvalidAmount() {
        com.parkease.payment.dto.request.SubscriptionPaymentRequest req = new com.parkease.payment.dto.request.SubscriptionPaymentRequest();
        req.setDriverEmail("v@t.com");
        req.setAmount(-10.0);
        service.initializeSubscriptionPayment(req);
        verify(repo, never()).save(any());
    }

    @Test
    void initializePayment_ExistingPending() {
        payment.setStatus(PaymentStatus.PENDING);
        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(200.0);
        service.initializePayment(req, "v@t.com");
        verify(repo).save(payment);
        assertEquals(200.0, payment.getAmount());
    }
}