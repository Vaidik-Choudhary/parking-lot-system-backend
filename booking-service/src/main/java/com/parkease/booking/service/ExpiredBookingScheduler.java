package com.parkease.booking.service;

import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.messaging.NotificationEvent;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredBookingScheduler {

    private final BookingRepository    repo;
    private final SpotServiceClient    spotServiceClient;
    private final LotServiceClient     lotServiceClient;
    private final NotificationPublisher notificationPublisher;

    @Value("${app.booking.checkin-grace-minutes}")
    private int graceMinutes;

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

                spotServiceClient.releaseSpot(booking.getSpotId());
                lotServiceClient.incrementAvailable(booking.getLotId());

                notificationPublisher.publish(NotificationEvent.builder()
                        .recipientEmail(booking.getDriverEmail())
                        .type("EXPIRY_REMINDER")
                        .title("Booking Auto-Cancelled")
                        .message("Your booking #" + booking.getBookingId()
                                + " was automatically cancelled because the check-in grace period"
                                + " of " + graceMinutes + " minutes expired.")
                        .relatedId(booking.getBookingId())
                        .relatedType("BOOKING")
                        .build());

                log.info("Auto-cancelled booking {} (spot: {}, lot: {})",
                        booking.getBookingId(), booking.getSpotId(), booking.getLotId());

            } catch (Exception e) {
                log.error("Failed to auto-cancel booking {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
    }
}
