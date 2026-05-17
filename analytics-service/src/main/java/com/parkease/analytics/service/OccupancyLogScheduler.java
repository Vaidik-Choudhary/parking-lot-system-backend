package com.parkease.analytics.service;

import com.parkease.analytics.client.BookingServiceClient;
import com.parkease.analytics.client.SpotServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OccupancyLogScheduler {

    private final AnalyticsService   analyticsService;
    private final SpotServiceClient  spotServiceClient;
    private final BookingServiceClient bookingServiceClient;

    @Scheduled(fixedDelay = 300000) // Log every 5 minutes
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
                    List<Map<String, Object>> spots = spotServiceClient.getSpotsByLot(lotId);
                    if (spots == null) continue;

                    int total = spots.size();
                    int occupied = (int) spots.stream()
                            .filter(s -> "OCCUPIED".equals(s.get("status")) || "RESERVED".equals(s.get("status")))
                            .count();

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

    private List<Long> fetchActiveLotIds() {
        try {
            List<Long> lotIds = bookingServiceClient.getDistinctLotIds();
            return lotIds != null ? lotIds : List.of();
        } catch (Exception e) {
            log.error("Could not fetch lot IDs: {}", e.getMessage());
            return List.of();
        }
    }
}
