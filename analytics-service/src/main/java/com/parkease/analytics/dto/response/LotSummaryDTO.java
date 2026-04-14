package com.parkease.analytics.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class LotSummaryDTO {
    private Long lotId;
    private String lotName;


    private double currentOccupancyRate;
    private int occupiedSpots;
    private int totalSpots;

   
    private double revenueToday;
    private double revenueThisMonth;
    private double revenueAllTime;

   
    private long bookingsToday;
    private long bookingsThisMonth;
    private long bookingsAllTime;

   
    private double avgParkingDurationMinutes;

    private List<Integer> peakHours;

    private Map<String, Double> spotTypeUtilisation;
}
