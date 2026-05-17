package com.parkease.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkease.payment.dto.request.*;
import com.parkease.payment.dto.response.*;
import com.parkease.payment.entity.PaymentStatus;
import com.parkease.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
    @Mock PaymentService service;
    @InjectMocks PaymentController controller;
    MockMvc mvc;
    ObjectMapper om;
    PaymentResponseDTO resp;
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("d@t.com", null, Collections.emptyList());

    @BeforeEach void setUp() {
        om = new ObjectMapper(); om.registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = PaymentResponseDTO.builder().paymentId(1L).bookingId(10L)
                .driverEmail("d@t.com").amount(200.0).currency("INR")
                .status(PaymentStatus.PAID).razorpayOrderId("order_123").build();
    }

    @Test void createOrder_returns201() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(10L); req.setAmount(200.0); req.setDescription("Parking");
        OrderResponseDTO orderResp = OrderResponseDTO.builder().paymentId(1L)
                .razorpayOrderId("order_123").amount(200.0).currency("INR")
                .status("created").razorpayKeyId("rzp_key").build();
        when(service.createOrder(any(), anyString())).thenReturn(orderResp);
        mvc.perform(post("/api/payments/order").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test void verifyPayment_returns200() throws Exception {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig_123");
        when(service.verifyPayment(any())).thenReturn(resp);
        mvc.perform(post("/api/payments/verify").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test void refund_returns200() throws Exception {
        RefundRequest req = new RefundRequest();
        req.setBookingId(10L); req.setReason("Cancelled");
        resp.setStatus(PaymentStatus.REFUNDED);
        when(service.refundPayment(any())).thenReturn(resp);
        mvc.perform(post("/api/payments/refund").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test void getMyPayments_returns200() throws Exception {
        when(service.getMyPayments("d@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/payments/my").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(1));
    }

    @Test void getByBooking_returns200() throws Exception {
        when(service.getPaymentByBookingId(10L)).thenReturn(resp);
        mvc.perform(get("/api/payments/booking/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(10));
    }

    @Test void getById_returns200() throws Exception {
        when(service.getPaymentById(1L)).thenReturn(resp);
        mvc.perform(get("/api/payments/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test void getAllPayments_returns200() throws Exception {
        when(service.getAllPayments()).thenReturn(List.of(resp));
        mvc.perform(get("/api/payments/admin/all")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(1));
    }

    @Test void webhook_returns200() throws Exception {
        mvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"event\":\"payment.captured\"}")
                .header("X-Razorpay-Signature","sig123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void initializePayment_returns200() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(10L); req.setAmount(200.0);
        doNothing().when(service).initializePayment(any(), anyString());
        mvc.perform(post("/api/payments/initialize").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void getPendingCount_returns200() throws Exception {
        when(service.getPendingPaymentsCount("d@t.com")).thenReturn(5L);
        mvc.perform(get("/api/payments/pending/count").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test void downloadReceipt_returns404() throws Exception {
        when(service.getReceiptPath(1L, "d@t.com")).thenReturn("invalid_path.pdf");
        mvc.perform(get("/api/payments/1/receipt").principal(auth))
                .andExpect(status().isNotFound());
    }

    @Test void downloadReceipt_returns200() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("receipt", ".pdf");
        tempFile.deleteOnExit();
        when(service.getReceiptPath(1L, "d@t.com")).thenReturn(tempFile.getAbsolutePath());
        mvc.perform(get("/api/payments/1/receipt").principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=receipt_1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test void initializeSubscriptionPayment_returns200() throws Exception {
        SubscriptionPaymentRequest req = new SubscriptionPaymentRequest();
        req.setDriverEmail("d@t.com");
        req.setSubscriptionId(10L);
        req.setAmount(500.0);
        doNothing().when(service).initializeSubscriptionPayment(any());
        mvc.perform(post("/api/payments/subscription/initialize").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
