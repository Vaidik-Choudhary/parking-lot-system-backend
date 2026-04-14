package com.parkease.analytics.service;

import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private OccupancyLogRepository logRepo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bookingServiceUrl", "http://booking-service");
        ReflectionTestUtils.setField(service, "paymentServiceUrl", "http://payment-service");
    }

    @Test
    void shouldReturnZeroOccupancyWhenNoLogsFound() {
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(null);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertNotNull(result);
        assertEquals(0.0, result.getOccupancyRate());
        assertEquals(0, result.getOccupiedSpots());
    }

    @Test
    void shouldReturnOccupancyRateSuccessfully() {
        OccupancyLog log = OccupancyLog.builder()
                .lotId(1L)
                .occupiedSpots(8)
                .totalSpots(10)
                .occupancyRate(0.8)
                .build();

        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(log);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertEquals(0.8, result.getOccupancyRate());
        assertEquals(80.0, result.getOccupancyPercent());
        assertEquals(2, result.getAvailableSpots());
    }

    @Test
    void shouldReturnHourlyOccupancyWithDefaults() {
        List<Object[]> rows = List.of(
                new Object[]{9, 0.85},
                new Object[]{10, 0.92}
        );

        when(logRepo.getHourlyOccupancy(1L)).thenReturn(rows);

        Map<Integer, Double> result = service.getHourlyOccupancy(1L);

        assertEquals(24, result.size());
        assertEquals(0.85, result.get(9));
        assertEquals(0.92, result.get(10));
        assertEquals(0.0, result.get(0));
    }

    @Test
    void shouldReturnPeakHoursSuccessfully() {
        List<Object[]> rows = List.of(
                new Object[]{10, 0.95},
                new Object[]{11, 0.90},
                new Object[]{9, 0.88}
        );

        when(logRepo.getPeakHours(1L)).thenReturn(rows);

        List<Integer> result = service.getPeakHours(1L, 2);

        assertEquals(List.of(10, 11), result);
    }

    @Test
    void shouldReturnRevenueReportSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "createdAt", "2026-04-14T10:00:00",
                        "totalAmount", 150.0
                ),
                Map.of(
                        "status", "COMPLETED",
                        "createdAt", "2026-04-14T12:00:00",
                        "totalAmount", 50.0
                )
        };

        when(restTemplate.exchange(
                contains("/api/bookings/lot/1"),
                any(),
                any(),
                eq(Map[].class)
        )).thenReturn(ResponseEntity.ok(bookings));

        RevenueReportDTO result = service.getRevenueReport(
                1L,
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                "Bearer token"
        );

        assertEquals(200.0, result.getTotalRevenue());
        assertEquals(2, result.getCompletedBookings());
    }

    @Test
    void shouldReturnAverageParkingDurationSuccessfully() {
        Map[] bookings = new Map[]{
                Map.of(
                        "status", "COMPLETED",
                        "checkInTime", "2026-04-14T10:00:00",
                        "checkOutTime", "2026-04-14T12:00:00"
                )
        };

        when(restTemplate.exchange(
                contains("/api/bookings/lot/1"),
                any(),
                any(),
                eq(Map[].class)
        )).thenReturn(ResponseEntity.ok(bookings));

        double result = service.getAvgParkingDuration(1L, "Bearer token");

        assertEquals(120.0, result);
    }

    @Test
    void shouldLogOccupancySuccessfully() {
        service.logOccupancy(1L, 8, 10);

        verify(logRepo).save(any(OccupancyLog.class));
    }

    @Test
    void shouldReturnPlatformSummarySuccessfully() {
        List<Object[]> latestLogs = List.of(
                new Object[]{1L, LocalDateTime.now(), 8, 10},
                new Object[]{2L, LocalDateTime.now(), 5, 10}
        );

        Map[] allBookings = new Map[]{
                Map.of(
                        "createdAt", LocalDate.now() + "T10:00:00",
                        "vehicleType", "CAR"
                )
        };

        Map[] payments = new Map[]{
                Map.of(
                        "status", "PAID",
                        "paidAt", LocalDate.now() + "T12:00:00",
                        "amount", 200.0
                )
        };

        when(logRepo.getLatestOccupancyAllLots()).thenReturn(latestLogs);
        when(restTemplate.getForObject(contains("/api/bookings/admin/all"), eq(Map[].class)))
                .thenReturn(allBookings);
        when(restTemplate.getForObject(contains("/api/payments/admin/all"), eq(Map[].class)))
                .thenReturn(payments);

        PlatformSummaryDTO result = service.getPlatformSummary("Bearer token");

        assertEquals(2, result.getTotalActiveLots());
        assertEquals(20, result.getTotalSpots());
        assertEquals(13, result.getTotalOccupiedSpots());
        assertEquals(200.0, result.getTotalRevenueToday());
    }
}