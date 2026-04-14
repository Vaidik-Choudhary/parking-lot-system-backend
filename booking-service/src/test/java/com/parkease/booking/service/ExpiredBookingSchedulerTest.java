package com.parkease.booking.service;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredBookingSchedulerTest {

    @Mock
    private BookingRepository repo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExpiredBookingScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "graceMinutes", 15);
        ReflectionTestUtils.setField(scheduler, "spotServiceUrl", "http://parkingspot-service");
        ReflectionTestUtils.setField(scheduler, "lotServiceUrl", "http://parkinglot-service");
    }

    @Test
    void shouldDoNothingWhenNoExpiredBookingsFound() {
        when(repo.findExpiredPreBookings(any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.cancelExpiredBookings();

        verify(repo, never()).save(any());
        verify(restTemplate, never()).put(anyString(), any());
    }

    @Test
    void shouldCancelExpiredBookingsAndReleaseResources() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .spotId(101L)
                .lotId(10L)
                .status(BookingStatus.RESERVED)
                .build();

        when(repo.findExpiredPreBookings(any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        scheduler.cancelExpiredBookings();

        verify(repo).save(booking);

        verify(restTemplate).put(
                contains("/api/spots/101/release"),
                isNull()
        );

        verify(restTemplate).put(
                contains("/api/lots/10/increment"),
                isNull()
        );
    }

    @Test
    void shouldContinueProcessingEvenIfOneBookingFails() {
        Booking booking1 = Booking.builder()
                .bookingId(1L)
                .spotId(101L)
                .lotId(10L)
                .status(BookingStatus.RESERVED)
                .build();

        Booking booking2 = Booking.builder()
                .bookingId(2L)
                .spotId(102L)
                .lotId(11L)
                .status(BookingStatus.RESERVED)
                .build();

        when(repo.findExpiredPreBookings(any(LocalDateTime.class)))
                .thenReturn(List.of(booking1, booking2));

        doThrow(new RuntimeException("spot-service down"))
                .when(restTemplate)
                .put(contains("/api/spots/101/release"), isNull());

        scheduler.cancelExpiredBookings();

        verify(repo, times(2)).save(any(Booking.class));
    }

    @Test
    void shouldMarkBookingAsCancelledBeforeSaving() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .spotId(101L)
                .lotId(10L)
                .status(BookingStatus.RESERVED)
                .build();

        when(repo.findExpiredPreBookings(any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        scheduler.cancelExpiredBookings();

        assert booking.getStatus() == BookingStatus.CANCELLED;
        verify(repo).save(booking);
    }
}
