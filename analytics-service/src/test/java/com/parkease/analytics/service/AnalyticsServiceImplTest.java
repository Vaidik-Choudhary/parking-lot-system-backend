package com.parkease.analytics.service;

import com.parkease.analytics.client.BookingServiceClient;
import com.parkease.analytics.client.PaymentServiceClient;
import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private BookingServiceClient bookingServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private com.parkease.analytics.client.SpotServiceClient spotServiceClient;

    @InjectMocks
    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldReturnZeroOccupancyWhenNoLogsFound() {
        lenient().when(spotServiceClient.getSpotsByLot(1L)).thenReturn(null);
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(null);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertNotNull(result);
        assertEquals(0.0, result.getOccupancyRate());
        assertEquals(0, result.getOccupiedSpots());
    }

    @Test
    void shouldReturnOccupancyRateSuccessfully() {
        lenient().when(spotServiceClient.getSpotsByLot(1L)).thenReturn(null);
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
        List<Map<String, Object>> bookings = List.of(
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
        );

        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

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
        List<Map<String, Object>> bookings = List.of(
                Map.of(
                        "status", "COMPLETED",
                        "checkInTime", "2026-04-14T10:00:00",
                        "checkOutTime", "2026-04-14T12:00:00"
                )
        );

        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

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

        List<Map<String, Object>> allBookings = List.of(
                Map.of(
                        "createdAt", LocalDate.now() + "T10:00:00",
                        "vehicleType", "CAR"
                )
        );

        List<Map<String, Object>> payments = List.of(
                Map.of(
                        "status", "PAID",
                        "paidAt", LocalDate.now() + "T12:00:00",
                        "amount", 200.0
                )
        );

        when(logRepo.getLatestOccupancyAllLots()).thenReturn(latestLogs);
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(allBookings);
        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(payments);

        PlatformSummaryDTO result = service.getPlatformSummary("Bearer token");

        assertEquals(2, result.getTotalActiveLots());
        assertEquals(20, result.getTotalSpots());
        assertEquals(13, result.getTotalOccupiedSpots());
        assertEquals(200.0, result.getTotalRevenueToday());
    }

    @Test
    void getSpotTypeUtilisation_Success() {
        List<Map<String, Object>> bookings = List.of(
                Map.of("status", "COMPLETED", "vehicleType", "FOUR_WHEELER"),
                Map.of("status", "COMPLETED", "vehicleType", "FOUR_WHEELER"),
                Map.of("status", "COMPLETED", "vehicleType", "TWO_WHEELER")
        );
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

        Map<String, Double> result = service.getSpotTypeUtilisation(1L, "token");
        assertEquals(2, result.size());
        assertEquals(66.67, result.get("FOUR_WHEELER"));
        assertEquals(33.33, result.get("TWO_WHEELER"));
    }

    @Test
    void getPlatformSummary_Fallback_WhenLogsEmpty() {
        when(logRepo.getLatestOccupancyAllLots()).thenReturn(List.of());
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of(1L));
        
        lenient().when(spotServiceClient.getSpotsByLot(1L)).thenReturn(null);
        OccupancyLog latestLog = OccupancyLog.builder().occupiedSpots(5).totalSpots(10).build();
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(latestLog);
        
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(List.of());
        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(List.of());

        PlatformSummaryDTO result = service.getPlatformSummary("token");
        
        assertEquals(1, result.getTotalActiveLots());
        assertEquals(10, result.getTotalSpots());
        assertEquals(5, result.getTotalOccupiedSpots());
    }

    @Test
    void getAvgParkingDuration_WithParseError() {
        List<Map<String, Object>> bookings = List.of(
                Map.of(
                        "status", "COMPLETED",
                        "checkInTime", "invalid-date",
                        "checkOutTime", "invalid-date"
                )
        );
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

        double result = service.getAvgParkingDuration(1L, "token");
        assertEquals(0.0, result);
    }

    @Test
    void fetchTotalRevenue_WithException() {
        when(logRepo.getLatestOccupancyAllLots()).thenReturn(List.of());
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of());
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(List.of());
        when(paymentServiceClient.getAllPayments(anyString())).thenThrow(new RuntimeException("API error"));
        
        PlatformSummaryDTO result = service.getPlatformSummary("token");
        assertEquals(0.0, result.getTotalRevenueToday());
    }

    @Test
    void getPlatformSummary_WithDifferentVehicleTypes() {
        Object[] logData = new Object[]{1L, LocalDateTime.now(), 8, 10};
        List<Object[]> latestLogs = java.util.Collections.singletonList(logData);
        List<Map<String, Object>> allBookings = List.of(
                Map.of("vehicleType", "CAR"),
                Map.of("vehicleType", "BIKE"),
                Map.of("vehicleType", "CAR")
        );
        when(logRepo.getLatestOccupancyAllLots()).thenReturn(latestLogs);
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(allBookings);
        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(List.of());

        PlatformSummaryDTO result = service.getPlatformSummary("token");
        assertEquals(2L, result.getBookingsByVehicleType().get("CAR"));
        assertEquals(1L, result.getBookingsByVehicleType().get("BIKE"));
    }

    @Test
    void getLotSummary_Success() {
        lenient().when(spotServiceClient.getSpotsByLot(1L)).thenReturn(null);
        OccupancyLog log = OccupancyLog.builder().lotId(1L).occupiedSpots(8).totalSpots(10).occupancyRate(0.8).build();
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(log);
        when(logRepo.getPeakHours(1L)).thenReturn(java.util.Collections.singletonList(new Object[]{10, 0.95}));
        
        List<Map<String, Object>> bookings = List.of(
                Map.of("status", "COMPLETED", "checkInTime", LocalDate.now() + "T10:00:00", "checkOutTime", LocalDate.now() + "T12:00:00", "vehicleType", "FOUR_WHEELER", "createdAt", LocalDate.now() + "T10:00:00", "totalAmount", 100.0)
        );
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);
        
        LotSummaryDTO summary = service.getLotSummary(1L, "token");
        assertNotNull(summary);
        assertEquals(0.8, summary.getCurrentOccupancyRate());
    }

    @Test
    void getOccupancyRate_WithRealtimeData() {
        List<Map<String, Object>> spots = List.of(
            Map.of("status", "OCCUPIED"),
            Map.of("status", "AVAILABLE"),
            Map.of("status", "RESERVED")
        );
        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(spots);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertEquals(2, result.getOccupiedSpots());
        assertEquals(3, result.getTotalSpots());
        assertEquals(1, result.getAvailableSpots());
    }

    @Test
    void getOccupancyRate_ExceptionInSpotClient() {
        when(spotServiceClient.getSpotsByLot(1L)).thenThrow(new RuntimeException("API error"));
        when(logRepo.findTopByLotIdOrderByTimestampDesc(1L)).thenReturn(null);

        OccupancyRateDTO result = service.getOccupancyRate(1L);

        assertEquals(0.0, result.getOccupancyRate());
    }

    @Test
    void getPlatformSummary_Fallback_WithActiveLotsAndRealtimeOccupancy() {
        when(logRepo.getLatestOccupancyAllLots()).thenReturn(List.of());
        when(bookingServiceClient.getDistinctLotIds()).thenReturn(List.of(1L, 2L));
        
        List<Map<String, Object>> spots = List.of(Map.of("status", "OCCUPIED"));
        when(spotServiceClient.getSpotsByLot(anyLong())).thenReturn(spots);
        
        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(List.of());
        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(List.of());

        PlatformSummaryDTO result = service.getPlatformSummary("token");
        
        assertEquals(2, result.getTotalActiveLots());
        assertEquals(2, result.getTotalSpots());
        assertEquals(2, result.getTotalOccupiedSpots());
    }

    @Test
    void getAvgParkingDuration_WithNullTimes() {
        List<Map<String, Object>> bookings = List.of(
                Map.of("status", "COMPLETED") // Missing checkIn/checkOut
        );
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

        double result = service.getAvgParkingDuration(1L, "token");
        assertEquals(0.0, result);
    }

    @Test
    void getRevenueReport_WithNullCreatedAt() {
        List<Map<String, Object>> bookings = List.of(
                Map.of("status", "COMPLETED") // Missing createdAt
        );
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(bookings);

        RevenueReportDTO result = service.getRevenueReport(1L, LocalDate.now(), LocalDate.now(), "token");
        assertEquals(0.0, result.getTotalRevenue());
    }

    @Test
    void fetchHelpers_Exceptions() {
        when(bookingServiceClient.getDistinctLotIds()).thenThrow(new RuntimeException("Error"));
        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenThrow(new RuntimeException("Error"));
        when(bookingServiceClient.getAllBookings(anyString())).thenThrow(new RuntimeException("Error"));
        
        // This will trigger all the catch blocks in the private helpers
        assertEquals(0.0, service.getAvgParkingDuration(1L, "token"));
        assertEquals(0, service.getPlatformSummary("token").getTotalActiveLots());
    }
}