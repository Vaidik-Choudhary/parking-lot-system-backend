package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.Map;

@FeignClient(name = "PARKINGLOT-SERVICE")
public interface LotServiceClient {

    @GetMapping("/api/lots/{lotId}")
    Map<String, Object> getLotById(@PathVariable("lotId") Long lotId);

    @PutMapping("/api/lots/{lotId}/decrement")
    void decrementAvailable(@PathVariable("lotId") Long lotId);

    @PutMapping("/api/lots/{lotId}/increment")
    void incrementAvailable(@PathVariable("lotId") Long lotId);
}
