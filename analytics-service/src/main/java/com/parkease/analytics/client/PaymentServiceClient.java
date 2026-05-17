package com.parkease.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/admin/all")
    List<Map<String, Object>> getAllPayments(
            @RequestHeader("Authorization") String authorization);
}
