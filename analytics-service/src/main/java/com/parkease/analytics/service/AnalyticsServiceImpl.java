package com.parkease.analytics.service;

import com.parkease.analytics.dto.response.*;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.repository.OccupancyLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OccupancyLogRepository logRepo;
    private final RestTemplate restTemplate;

    @Value("${app.services.booking-service}")
    private String bookingServiceUrl;

    @Value("${app.services.payment-service}")
    private String paymentServiceUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public OccupancyRateDTO getOccupancyRate(Long lotId) {
        log.debug("Getting occupancy rate for lot: {}", lotId);

        OccupancyLog latest = logRepo.findTopByLotIdOrderByTimestampDesc(lotId);

        if (latest == null) {
            return OccupancyRateDTO.builder()
                    .lotId(lotId)
                    .occupiedSpots(0).totalSpots(0)
                    .occupancyRate(0.0).occupancyPercent(0.0)
                    .availableSpots(0).build();
        }

        double percent = Math.round(latest.getOccupancyRate() * 100.0 * 100.0) / 100.0;

        return OccupancyRateDTO.builder()
                .lotId(lotId)
                .occupiedSpots(latest.getOccupiedSpots())
                .totalSpots(latest.getTotalSpots())
                .occupancyRate(latest.getOccupancyRate())
                .occupancyPercent(percent)
                .availableSpots(latest.getTotalSpots() - latest.getOccupiedSpots())
                .build();
    }

    @Override
    public Map<Integer, Double> getHourlyOccupancy(Long lotId) {
        log.debug("Getting hourly occupancy for lot: {}", lotId);

        List<Object[]> results = logRepo.getHourlyOccupancy(lotId);
        Map<Integer, Double> hourlyMap = new LinkedHashMap<>();

        for (int h = 0; h < 24; h++) hourlyMap.put(h, 0.0);

        for (Object[] row : results) {
            int hour      = ((Number) row[0]).intValue();
            double avgRate = ((Number) row[1]).doubleValue();
            hourlyMap.put(hour, Math.round(avgRate * 100.0) / 100.0);
        }

        return hourlyMap;
    }

    @Override
    public List<Integer> getPeakHours(Long lotId, int topN) {
        log.debug("Getting peak hours for lot: {} (top {})", lotId, topN);

        List<Object[]> results = logRepo.getPeakHours(lotId);

        return results.stream()
                .limit(topN)
                .map(row -> ((Number) row[0]).intValue())
                .collect(Collectors.toList());
    }

    @Override
    public RevenueReportDTO getRevenueReport(Long lotId, LocalDate from, LocalDate to, String token) {
        log.debug("Getting revenue report for lot: {} from {} to {}", lotId, from, to);

        List<Map> bookings = fetchBookingsForLot(lotId, token);

        List<Map> completed = bookings.stream()
                .filter(b -> "COMPLETED".equals(b.get("status")))
                .filter(b -> {
                    String createdAt = (String) b.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate date = LocalDate.parse(createdAt.substring(0, 10));
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();

        Map<String, Double> revenueByDay = new TreeMap<>();
        double totalRevenue = 0.0;

        for (Map booking : completed) {
            String dateStr = ((String) booking.get("createdAt")).substring(0, 10);
            double amount  = booking.get("totalAmount") != null
                    ? ((Number) booking.get("totalAmount")).doubleValue() : 0.0;
            revenueByDay.merge(dateStr, amount, Double::sum);
            totalRevenue += amount;
        }

        return RevenueReportDTO.builder()
                .lotId(lotId)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .totalBookings(bookings.size())
                .completedBookings(completed.size())
                .revenueByDay(revenueByDay)
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .build();
    }

    @Override
    public Map<String, Double> getSpotTypeUtilisation(Long lotId, String token) {
        log.debug("Getting spot type utilisation for lot: {}", lotId);

        List<Map> bookings = fetchBookingsForLot(lotId, token);

        Map<String, Long> countByType = new HashMap<>();
        long total = 0;

        for (Map booking : bookings) {
            if ("COMPLETED".equals(booking.get("status"))) {
        
                String vehicleType = (String) booking.get("vehicleType");
                if (vehicleType != null) {
                    countByType.merge(vehicleType, 1L, Long::sum);
                    total++;
                }
            }
        }

        if (total == 0) return Collections.emptyMap();

        final long finalTotal = total;
        Map<String, Double> utilisation = new LinkedHashMap<>();
        countByType.forEach((type, count) ->
            utilisation.put(type, Math.round((count * 100.0 / finalTotal) * 100.0) / 100.0)
        );

        return utilisation;
    }

    @Override
    public double getAvgParkingDuration(Long lotId, String token) {
        log.debug("Getting avg parking duration for lot: {}", lotId);

        List<Map> bookings = fetchBookingsForLot(lotId, token);

        List<Map> completed = bookings.stream()
                .filter(b -> "COMPLETED".equals(b.get("status"))
                          && b.get("checkInTime") != null
                          && b.get("checkOutTime") != null)
                .toList();

        if (completed.isEmpty()) return 0.0;

        double totalMinutes = 0;
        for (Map booking : completed) {
            try {
                LocalDateTime checkIn  = LocalDateTime.parse(
                        ((String) booking.get("checkInTime")).replace("Z", ""));
                LocalDateTime checkOut = LocalDateTime.parse(
                        ((String) booking.get("checkOutTime")).replace("Z", ""));
                totalMinutes += java.time.Duration.between(checkIn, checkOut).toMinutes();
            } catch (Exception e) {
                log.warn("Could not parse booking times: {}", e.getMessage());
            }
        }

        return Math.round((totalMinutes / completed.size()) * 100.0) / 100.0;
    }

    @Override
    public LotSummaryDTO getLotSummary(Long lotId, String token) {
        log.debug("Getting full summary for lot: {}", lotId);

        OccupancyRateDTO occupancy = getOccupancyRate(lotId);
        List<Integer> peakHours   = getPeakHours(lotId, 3);
        double avgDuration        = getAvgParkingDuration(lotId,token);
        Map<String, Double> spotUtil = getSpotTypeUtilisation(lotId,token);

        LocalDate today     = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        RevenueReportDTO todayRevenue  = getRevenueReport(lotId, today, today, token);
        RevenueReportDTO monthRevenue  = getRevenueReport(lotId, monthStart, today, token);
        RevenueReportDTO allTimeRevenue = getRevenueReport(lotId, LocalDate.of(2020, 1, 1), today, token);

        return LotSummaryDTO.builder()
                .lotId(lotId)
                .currentOccupancyRate(occupancy.getOccupancyRate())
                .occupiedSpots(occupancy.getOccupiedSpots())
                .totalSpots(occupancy.getTotalSpots())
                .revenueToday(todayRevenue.getTotalRevenue())
                .revenueThisMonth(monthRevenue.getTotalRevenue())
                .revenueAllTime(allTimeRevenue.getTotalRevenue())
                .bookingsToday(todayRevenue.getCompletedBookings())
                .bookingsThisMonth(monthRevenue.getCompletedBookings())
                .bookingsAllTime(allTimeRevenue.getTotalBookings())
                .avgParkingDurationMinutes(avgDuration)
                .peakHours(peakHours)
                .spotTypeUtilisation(spotUtil)
                .build();
    }

    @Override
    public PlatformSummaryDTO getPlatformSummary( String token) {
        log.debug("Computing platform-wide summary");

        List<Object[]> latestLogs = logRepo.getLatestOccupancyAllLots();

        int totalLots     = latestLogs.size();
        int totalSpots    = 0;
        int totalOccupied = 0;

        for (Object[] row : latestLogs) {
            totalOccupied += ((Number) row[2]).intValue();
            totalSpots    += ((Number) row[3]).intValue();
        }

        double platformRate = totalSpots > 0
                ? Math.round((totalOccupied * 100.0 / totalSpots) * 100.0) / 100.0 : 0.0;

        List<Map> allBookings = fetchAllBookings();

        LocalDate today = LocalDate.now();
        long bookingsToday = allBookings.stream()
                .filter(b -> {
                    String ca = (String) b.get("createdAt");
                    return ca != null && ca.startsWith(today.format(DATE_FMT));
                }).count();

        double revenueToday    = fetchTotalRevenue(today, today);
        double revenueAllTime  = fetchTotalRevenue(LocalDate.of(2020,1,1), today);

        Map<String, Long> byCity = new HashMap<>();
        Map<String, Long> byVehicleType = new HashMap<>();

        for (Map booking : allBookings) {
            String vt = (String) booking.get("vehicleType");
            if (vt != null) byVehicleType.merge(vt, 1L, Long::sum);
        }

        return PlatformSummaryDTO.builder()
                .totalActiveLots(totalLots)
                .totalSpots(totalSpots)
                .totalOccupiedSpots(totalOccupied)
                .platformOccupancyRate(platformRate)
                .totalBookingsToday(bookingsToday)
                .totalBookingsAllTime(allBookings.size())
                .totalRevenueToday(revenueToday)
                .totalRevenueAllTime(revenueAllTime)
                .bookingsByCity(byCity)
                .bookingsByVehicleType(byVehicleType)
                .build();
    }

    @Override
    public void logOccupancy(Long lotId, int occupiedSpots, int totalSpots) {
        LocalDateTime now = LocalDateTime.now();
        double rate = totalSpots > 0 ? (double) occupiedSpots / totalSpots : 0.0;

        OccupancyLog log = OccupancyLog.builder()
                .lotId(lotId)
                .timestamp(now)
                .occupancyRate(Math.round(rate * 10000.0) / 10000.0)
                .occupiedSpots(occupiedSpots)
                .totalSpots(totalSpots)
                .hourOfDay(now.getHour())
                .dayOfWeek(now.getDayOfWeek().getValue())
                .build();

        logRepo.save(log);
    }

    @SuppressWarnings("unchecked")
    private List<Map> fetchBookingsForLot(Long lotId, String bearerToken) {
        try {
            String url = bookingServiceUrl + "/api/bookings/lot/" + lotId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map[].class);

            Map[] result = response.getBody();
            return result != null ? Arrays.asList(result) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not fetch bookings for lot {}: {}", lotId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map> fetchAllBookings() {
        try {
            String url = bookingServiceUrl + "/api/bookings/admin/all";
            Map[] result = restTemplate.getForObject(url, Map[].class);
            return result != null ? Arrays.asList(result) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not fetch all bookings: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double fetchTotalRevenue(LocalDate from, LocalDate to) {
        try {
            String url = paymentServiceUrl + "/api/payments/admin/all";
            Map[] payments = restTemplate.getForObject(url, Map[].class);
            if (payments == null) return 0.0;

            return Arrays.stream(payments)
                    .filter(p -> "PAID".equals(p.get("status")))
                    .filter(p -> {
                        String paidAt = (String) p.get("paidAt");
                        if (paidAt == null) return false;
                        LocalDate date = LocalDate.parse(paidAt.substring(0, 10));
                        return !date.isBefore(from) && !date.isAfter(to);
                    })
                    .mapToDouble(p -> p.get("amount") != null
                            ? ((Number) p.get("amount")).doubleValue() : 0.0)
                    .sum();
        } catch (Exception e) {
            log.error("Could not fetch revenue: {}", e.getMessage());
            return 0.0;
        }
    }
}
