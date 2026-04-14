package com.parkease.analytics.dto.response;

import lombok.*;
import java.util.Map;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class OccupancyRateDTO {
    private Long lotId;
    private int occupiedSpots;
    private int totalSpots;
    private double occupancyRate;    
    private double occupancyPercent; 
    private int availableSpots;
}
