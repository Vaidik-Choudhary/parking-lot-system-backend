package com.parkease.analytics.service;

import com.parkease.analytics.client.BookingServiceClient;
import com.parkease.analytics.client.SpotServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupancyLogSchedulerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private SpotServiceClient spotServiceClient;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private OccupancyLogScheduler scheduler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldLogAllLotOccupanciesSuccessfully() {
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of(1L, 2L));

        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(List.of(
                Map.of("spotId", 1, "status", "OCCUPIED"),
                Map.of("spotId", 2, "status", "OCCUPIED"),
                Map.of("spotId", 3, "status", "AVAILABLE")));

        when(spotServiceClient.getSpotsByLot(2L)).thenReturn(List.of(
                Map.of("spotId", 1, "status", "OCCUPIED"),
                Map.of("spotId", 2, "status", "AVAILABLE")));

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(1L, 2, 3);
        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldDoNothingWhenNoActiveLotsFound() {
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of());

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }

    @Test
    void shouldContinueWhenOneLotFails() {
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of(1L, 2L));
        when(spotServiceClient.getSpotsByLot(1L)).thenThrow(new RuntimeException("spot service down"));

        when(spotServiceClient.getSpotsByLot(2L)).thenReturn(List.of(
                Map.of("spotId", 1, "status", "OCCUPIED"),
                Map.of("spotId", 2, "status", "AVAILABLE")));

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldHandleBookingServiceFailureGracefully() {
        when(bookingServiceClient.getDistinctLotIds()).thenThrow(new RuntimeException("booking service down"));

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }
}