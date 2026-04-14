package com.parkease.analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupancyLogSchedulerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OccupancyLogScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                scheduler,
                "spotServiceUrl",
                "http://parkingspot-service"
        );
        ReflectionTestUtils.setField(
                scheduler,
                "bookingServiceUrl",
                "http://booking-service"
        );
    }

    @Test
    void shouldLogAllLotOccupanciesSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of("lotId", 1L),
                Map.of("lotId", 2L)
        };

        Map[] spotsLot1 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2),
                Map.of("spotId", 3)
        };

        Map[] spotsLot2 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2)
        };

        when(restTemplate.getForObject(
                contains("/api/bookings/admin/all"),
                eq(Map[].class)
        )).thenReturn(bookings);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/1/count"),
                eq(Integer.class)
        )).thenReturn(1);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/1"),
                eq(Map[].class)
        )).thenReturn(spotsLot1);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/2/count"),
                eq(Integer.class)
        )).thenReturn(1);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/2"),
                eq(Map[].class)
        )).thenReturn(spotsLot2);

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(1L, 2, 3);
        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldDoNothingWhenNoActiveLotsFound() {
        when(restTemplate.getForObject(
                contains("/api/bookings/admin/all"),
                eq(Map[].class)
        )).thenReturn(new Map[0]);

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }

    @Test
    void shouldContinueWhenOneLotFails() {
        Map[] bookings = new Map[]{
                Map.of("lotId", 1L),
                Map.of("lotId", 2L)
        };

        Map[] spotsLot2 = new Map[]{
                Map.of("spotId", 1),
                Map.of("spotId", 2)
        };

        when(restTemplate.getForObject(
                contains("/api/bookings/admin/all"),
                eq(Map[].class)
        )).thenReturn(bookings);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/1/count"),
                eq(Integer.class)
        )).thenThrow(new RuntimeException("spot service down"));

        when(restTemplate.getForObject(
                contains("/api/spots/lot/2/count"),
                eq(Integer.class)
        )).thenReturn(1);

        when(restTemplate.getForObject(
                contains("/api/spots/lot/2"),
                eq(Map[].class)
        )).thenReturn(spotsLot2);

        scheduler.logAllLotOccupancies();

        verify(analyticsService).logOccupancy(2L, 1, 2);
    }

    @Test
    void shouldHandleBookingServiceFailureGracefully() {
        when(restTemplate.getForObject(
                contains("/api/bookings/admin/all"),
                eq(Map[].class)
        )).thenThrow(new RuntimeException("booking service down"));

        scheduler.logAllLotOccupancies();

        verify(analyticsService, never())
                .logOccupancy(anyLong(), anyInt(), anyInt());
    }
}