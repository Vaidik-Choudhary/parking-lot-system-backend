package com.parkease.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "BOOKING-SERVICE")
public interface BookingServiceClient {

    @GetMapping("/api/bookings/internal/lot-ids")
    List<Long> getDistinctLotIds();

    @GetMapping("/api/bookings/lot/{lotId}")
    List<Map<String, Object>> getBookingsByLot(
            @PathVariable("lotId") Long lotId,
            @RequestHeader("Authorization") String authorization);

    @GetMapping("/api/bookings/admin/all")
    List<Map<String, Object>> getAllBookings(
            @RequestHeader("Authorization") String authorization);
}
