package com.parkease.analytics.dto.response;

import lombok.*;
import java.util.Map;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class RevenueReportDTO {
    private Long lotId;
    private double totalRevenue;
    private int totalBookings;
    private int completedBookings;
    private Map<String, Double> revenueByDay;
    private String fromDate;
    private String toDate;
}
