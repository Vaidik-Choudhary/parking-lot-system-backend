package com.parkease.booking.service;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredBookingScheduler {

    private final BookingRepository repo;
    private final RestTemplate restTemplate;

    @Value("${app.booking.checkin-grace-minutes}")
    private int graceMinutes;

    @Value("${app.services.spot-service}")
    private String spotServiceUrl;

    @Value("${app.services.parkinglot-service}")
    private String lotServiceUrl;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(graceMinutes);

        List<Booking> expired = repo.findExpiredPreBookings(cutoff);

        if (expired.isEmpty()) {
            log.debug("Scheduler: no expired bookings found");
            return;
        }

        log.info("Scheduler: auto-cancelling {} expired pre-bookings", expired.size());

        for (Booking booking : expired) {
            try {
                booking.setStatus(BookingStatus.CANCELLED);
                repo.save(booking);

                restTemplate.put(spotServiceUrl + "/api/spots/" + booking.getSpotId() + "/release", null);

                restTemplate.put(lotServiceUrl + "/api/lots/" + booking.getLotId() + "/increment", null);

                log.info("Auto-cancelled booking {} (spot: {}, lot: {})",
                        booking.getBookingId(), booking.getSpotId(), booking.getLotId());

            } catch (Exception e) {
                log.error("Failed to auto-cancel booking {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
    }
}
