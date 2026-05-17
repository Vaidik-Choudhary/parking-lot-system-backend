package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/pending/count")
    long getPendingCount(@RequestHeader("Authorization") String token);

    @PostMapping("/api/payments/initialize")
    void initializePayment(@RequestHeader("Authorization") String token, @RequestBody java.util.Map<String, Object> request);

    @GetMapping("/api/payments/my")
    java.util.List<java.util.Map<String, Object>> getMyPayments(@RequestHeader("Authorization") String token);

    @GetMapping("/api/payments/booking/{bookingId}")
    java.util.Map<String, Object> getPaymentByBookingId(@RequestHeader("Authorization") String token, @PathVariable("bookingId") Long bookingId);

    @PostMapping("/api/payments/subscription/initialize")
    void initializeSubscriptionPayment(@RequestBody com.parkease.booking.dto.request.SubscriptionPaymentRequest request);
}
