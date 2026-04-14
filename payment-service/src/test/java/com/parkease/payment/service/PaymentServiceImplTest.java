package com.parkease.payment.service;

import com.parkease.payment.dto.request.CreateOrderRequest;
import com.parkease.payment.dto.request.RefundRequest;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentStatus;
import com.parkease.payment.exception.PaymentException;
import com.parkease.payment.mapper.PaymentMapper;
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
}