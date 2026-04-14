package com.parkease.analytics.service;

import com.parkease.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OccupancyLogScheduler {

    private final AnalyticsService analyticsService;
    private final RestTemplate restTemplate;

    @Value("${app.services.spot-service}")
    private String spotServiceUrl;

    @Value("${app.services.booking-service}")
    private String bookingServiceUrl;

    @Scheduled(fixedDelay = 1_800_000)  
    public void logAllLotOccupancies() {
        log.debug("Scheduler: logging occupancy snapshots for all lots");

        try {
           
            List<Long> lotIds = fetchActiveLotIds();

            if (lotIds.isEmpty()) {
                log.debug("Scheduler: no active lots found");
                return;
            }

            int logged = 0;
            for (Long lotId : lotIds) {
                try {
           
                    Integer available = restTemplate.getForObject(
                            spotServiceUrl + "/api/spots/lot/" + lotId + "/count",
                            Integer.class
                    );

                    Map[] spots = restTemplate.getForObject(
                            spotServiceUrl + "/api/spots/lot/" + lotId,
                            Map[].class
                    );

                    if (spots == null || available == null) continue;

                    int total    = spots.length;
                    int occupied = total - available;

                    analyticsService.logOccupancy(lotId, occupied, total);
                    logged++;

                } catch (Exception e) {
                    log.error("Failed to log occupancy for lot {}: {}", lotId, e.getMessage());
                }
            }

            log.info("Scheduler: logged occupancy for {} lots", logged);

        } catch (Exception e) {
            log.error("Occupancy scheduler failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> fetchActiveLotIds() {
        try {
            Map[] bookings = restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/admin/all", Map[].class);

            if (bookings == null) return List.of();

            return Arrays.stream(bookings)
                    .map(b -> b.get("lotId"))
                    .filter(id -> id != null)
                    .map(id -> ((Number) id).longValue())
                    .distinct()
                    .toList();

        } catch (Exception e) {
            log.error("Could not fetch lot IDs: {}", e.getMessage());
            return List.of();
        }
    }
}
