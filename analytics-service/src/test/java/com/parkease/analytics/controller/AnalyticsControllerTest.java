package com.parkease.analytics.controller;

import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {
    @Mock AnalyticsService service;
    @InjectMocks AnalyticsController controller;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test void getOccupancy_returns200() throws Exception {
        OccupancyRateDTO dto = OccupancyRateDTO.builder().lotId(1L).occupiedSpots(30)
                .totalSpots(50).occupancyRate(0.6).occupancyPercent(60.0).availableSpots(20).build();
        when(service.getOccupancyRate(1L)).thenReturn(dto);
        mvc.perform(get("/api/analytics/lots/1/occupancy")).andExpect(status().isOk())
                .andExpect(jsonPath("$.occupancyPercent").value(60.0));
    }

    @Test void getHourly_returns200() throws Exception {
        Map<Integer, Double> hourly = new TreeMap<>();
        hourly.put(9, 0.8); hourly.put(18, 0.95);
        when(service.getHourlyOccupancy(1L)).thenReturn(hourly);
        mvc.perform(get("/api/analytics/lots/1/hourly")).andExpect(status().isOk())
                .andExpect(jsonPath("$.9").value(0.8));
    }

    @Test void getPeakHours_returns200() throws Exception {
        when(service.getPeakHours(1L, 3)).thenReturn(List.of(9, 18, 12));
        mvc.perform(get("/api/analytics/lots/1/peak-hours").param("top", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(9));
    }

    @Test void getRevenue_returns200() throws Exception {
        RevenueReportDTO dto = RevenueReportDTO.builder().lotId(1L).totalRevenue(5000.0)
                .totalBookings(25).completedBookings(20).revenueByDay(new TreeMap<>())
                .fromDate("2026-04-01").toDate("2026-04-30").build();
        when(service.getRevenueReport(anyLong(), any(), any(), any())).thenReturn(dto);
        mvc.perform(get("/api/analytics/lots/1/revenue")
                .param("from","2026-04-01").param("to","2026-04-30")
                .header("Authorization","Bearer token123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(5000.0));
    }

    @Test void getRevenue_defaultDates_returns200() throws Exception {
        RevenueReportDTO dto = RevenueReportDTO.builder().lotId(1L).totalRevenue(1000.0)
                .totalBookings(5).completedBookings(4).revenueByDay(new TreeMap<>())
                .fromDate("2026-04-01").toDate("2026-04-30").build();
        when(service.getRevenueReport(anyLong(), any(), any(), any())).thenReturn(dto);
        mvc.perform(get("/api/analytics/lots/1/revenue").header("Authorization","Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test void getUtilisation_returns200() throws Exception {
        when(service.getSpotTypeUtilisation(anyLong(), any()))
                .thenReturn(Map.of("FOUR_WHEELER", 80.0));
        mvc.perform(get("/api/analytics/lots/1/utilisation").header("Authorization","Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test void getAvgDuration_returns200() throws Exception {
        when(service.getAvgParkingDuration(anyLong(), any())).thenReturn(90.0);
        mvc.perform(get("/api/analytics/lots/1/avg-duration").header("Authorization","Bearer token123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avgDurationMinutes").value(90.0));
    }

    @Test void getLotSummary_returns200() throws Exception {
        LotSummaryDTO dto = LotSummaryDTO.builder().lotId(1L).currentOccupancyRate(0.6)
                .occupiedSpots(30).totalSpots(50).revenueToday(500.0)
                .revenueThisMonth(5000.0).revenueAllTime(50000.0)
                .bookingsToday(5).bookingsThisMonth(50).bookingsAllTime(500)
                .avgParkingDurationMinutes(75.0).peakHours(List.of(9,18))
                .spotTypeUtilisation(Map.of()).build();
        when(service.getLotSummary(anyLong(), any())).thenReturn(dto);
        mvc.perform(get("/api/analytics/lots/1/summary").header("Authorization","Bearer token123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lotId").value(1));
    }

    @Test void getPlatformSummary_returns200() throws Exception {
        PlatformSummaryDTO dto = PlatformSummaryDTO.builder().totalActiveLots(10)
                .totalSpots(500).totalOccupiedSpots(300).platformOccupancyRate(60.0)
                .totalBookingsToday(50L).totalBookingsAllTime(5000L)
                .totalRevenueToday(2000.0).totalRevenueAllTime(200000.0)
                .bookingsByCity(Map.of()).bookingsByVehicleType(Map.of()).build();
        when(service.getPlatformSummary(any())).thenReturn(dto);
        mvc.perform(get("/api/analytics/platform").header("Authorization","Bearer token123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalActiveLots").value(10));
    }

    @Test void logOccupancy_returns200() throws Exception {
        doNothing().when(service).logOccupancy(1L, 30, 50);
        mvc.perform(post("/api/analytics/internal/log")
                .param("lotId","1").param("occupied","30").param("total","50"))
                .andExpect(status().isOk());
    }
}
