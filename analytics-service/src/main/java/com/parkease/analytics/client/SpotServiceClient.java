package com.parkease.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PARKINGSPOT-SERVICE")
public interface SpotServiceClient {

    @GetMapping("/api/spots/lot/{lotId}/count")
    Integer getAvailableSpotCount(@PathVariable("lotId") Long lotId);

    @GetMapping("/api/spots/lot/{lotId}")
    List<Map<String, Object>> getSpotsByLot(@PathVariable("lotId") Long lotId);
}
