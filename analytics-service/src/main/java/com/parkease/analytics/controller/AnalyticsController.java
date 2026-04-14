package com.parkease.analytics.controller;

import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService service;

    /**
     * GET /api/analytics/lots/{lotId}/occupancy
     * Current occupancy rate from the latest snapshot.
     */
    @GetMapping("/lots/{lotId}/occupancy")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<OccupancyRateDTO> getOccupancy(@PathVariable Long lotId) {
        log.info("GET /api/analytics/lots/{}/occupancy", lotId);
        return ResponseEntity.ok(service.getOccupancyRate(lotId));
    }

    /**
     * GET /api/analytics/lots/{lotId}/hourly
     * Average occupancy rate for each hour of the day (0-23).
     */
    @GetMapping("/lots/{lotId}/hourly")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<Integer, Double>> getHourly(@PathVariable Long lotId) {
        log.info("GET /api/analytics/lots/{}/hourly", lotId);
        return ResponseEntity.ok(service.getHourlyOccupancy(lotId));
    }

    /**
     * GET /api/analytics/lots/{lotId}/peak-hours?top=3
     * Returns the N busiest hours of the day, e.g. [9, 18, 10]
     */
    @GetMapping("/lots/{lotId}/peak-hours")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<Integer>> getPeakHours(
            @PathVariable Long lotId,
            @RequestParam(defaultValue = "3") int top) {
        log.info("GET /api/analytics/lots/{}/peak-hours?top={}", lotId, top);
        return ResponseEntity.ok(service.getPeakHours(lotId, top));
    }

    /**
     * GET /api/analytics/lots/{lotId}/revenue?from=2026-04-01&to=2026-04-13
     * Revenue report for a custom date range.
     */
    @GetMapping("/lots/{lotId}/revenue")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<RevenueReportDTO> getRevenue(
            @PathVariable Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,HttpServletRequest request) {

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : LocalDate.now();

        log.info("GET /api/analytics/lots/{}/revenue from={} to={}", lotId, start, end);
        String token = request.getHeader("Authorization");
        return ResponseEntity.ok(service.getRevenueReport(lotId, start, end, token));
    }

    /**
     * GET /api/analytics/lots/{lotId}/utilisation
     * How each vehicle/spot type is being used (as percentage).
     * Response: { "FOUR_WHEELER": 80.0, "TWO_WHEELER": 20.0 }
     */
    @GetMapping("/lots/{lotId}/utilisation")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Double>> getUtilisation(@PathVariable Long lotId,HttpServletRequest request) {
        log.info("GET /api/analytics/lots/{}/utilisation", lotId);
        String token = request.getHeader("Authorization");
        return ResponseEntity.ok(service.getSpotTypeUtilisation(lotId, token));
    }

    /**
     * GET /api/analytics/lots/{lotId}/avg-duration
     * Average parking duration in minutes across all completed bookings.
     */
    @GetMapping("/lots/{lotId}/avg-duration")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Double>> getAvgDuration(@PathVariable Long lotId,  HttpServletRequest request) {
        log.info("GET /api/analytics/lots/{}/avg-duration", lotId);
        String token = request.getHeader("Authorization");
        double avg = service.getAvgParkingDuration(lotId, token);
        return ResponseEntity.ok(Map.of(
                "avgDurationMinutes", avg,
                "avgDurationHours", Math.round(avg / 60.0 * 100.0) / 100.0
        ));
    }

    /**
     * GET /api/analytics/lots/{lotId}/summary
     * All metrics in one call — for the manager dashboard home page.
     */
    @GetMapping("/lots/{lotId}/summary")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<LotSummaryDTO> getLotSummary( @PathVariable Long lotId, HttpServletRequest request) {        // ← add this
        log.info("GET /api/analytics/lots/{}/summary", lotId);
        String token = request.getHeader("Authorization"); // ← extract token
        return ResponseEntity.ok(service.getLotSummary(lotId, token)); // ← pass token
    }

    /**
     * GET /api/analytics/platform
     * Platform-wide summary for the admin dashboard.
     */
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformSummaryDTO> getPlatformSummary(@PathVariable HttpServletRequest request) {
        log.info("GET /api/analytics/platform");
        String token = request.getHeader("Authorization");
        return ResponseEntity.ok(service.getPlatformSummary(token));
    }

    /**
     * POST /api/analytics/internal/log?lotId=1&occupied=38&total=50
     * Logs an occupancy snapshot.
     */
    @PostMapping("/internal/log")
    public ResponseEntity<Void> logOccupancy(
            @RequestParam Long lotId,
            @RequestParam int occupied,
            @RequestParam int total) {
        log.debug("POST /api/analytics/internal/log - lot: {} occ: {}/{}", lotId, occupied, total);
        service.logOccupancy(lotId, occupied, total);
        return ResponseEntity.ok().build();
    }
}
