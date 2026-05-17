package com.parkease.booking.service;

import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.entity.*;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredBookingSchedulerTest {

    @Mock BookingRepository repo;
    @Mock SpotServiceClient spotServiceClient;
    @Mock LotServiceClient lotServiceClient;
    @Mock NotificationPublisher notificationPublisher;

    @InjectMocks ExpiredBookingScheduler scheduler;

    Booking booking;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(scheduler, "graceMinutes", 15);
        booking = Booking.builder().bookingId(1L).driverEmail("d@t.com")
                .spotId(101L).lotId(10L).status(BookingStatus.RESERVED)
                .bookingType(BookingType.PRE_BOOKING)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1)).build();
    }

    @Test void shouldDoNothingWhenNoExpiredBookingsFound() {
        when(repo.findExpiredPreBookings(any())).thenReturn(List.of());
        scheduler.cancelExpiredBookings();
        verify(repo, never()).save(any());
    }

    @Test void shouldCancelExpiredBookingsAndReleaseResources() {
        when(repo.findExpiredPreBookings(any())).thenReturn(List.of(booking));
        when(repo.save(any())).thenReturn(booking);
        doNothing().when(spotServiceClient).releaseSpot(anyLong());
        doNothing().when(lotServiceClient).incrementAvailable(anyLong());
        doNothing().when(notificationPublisher).publish(any());

        scheduler.cancelExpiredBookings();

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(repo).save(booking);
        verify(spotServiceClient).releaseSpot(101L);
        verify(lotServiceClient).incrementAvailable(10L);
        verify(notificationPublisher).publish(any());
    }

    @Test void shouldContinueProcessingEvenIfOneBookingFails() {
        Booking booking2 = Booking.builder().bookingId(2L).driverEmail("d2@t.com")
                .spotId(102L).lotId(10L).status(BookingStatus.RESERVED)
                .bookingType(BookingType.PRE_BOOKING)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1)).build();

        when(repo.findExpiredPreBookings(any())).thenReturn(List.of(booking, booking2));
        when(repo.save(any())).thenReturn(booking).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> scheduler.cancelExpiredBookings());
    }

    @Test void shouldMarkBookingAsCancelledBeforeSaving() {
        when(repo.findExpiredPreBookings(any())).thenReturn(List.of(booking));
        when(repo.save(any())).thenReturn(booking);
        doNothing().when(spotServiceClient).releaseSpot(anyLong());
        doNothing().when(lotServiceClient).incrementAvailable(anyLong());
        doNothing().when(notificationPublisher).publish(any());

        scheduler.cancelExpiredBookings();

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }
}
