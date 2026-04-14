package com.parkease.analytics.service;

import com.parkease.analytics.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {

	OccupancyRateDTO getOccupancyRate(Long lotId);
	Map<Integer, Double> getHourlyOccupancy(Long lotId);
	List<Integer> getPeakHours(Long lotId, int topN);
	RevenueReportDTO getRevenueReport(Long lotId, LocalDate from, LocalDate to, String token); 
	Map<String, Double> getSpotTypeUtilisation(Long lotId, String token);                     
	double getAvgParkingDuration(Long lotId, String token);                                    
	LotSummaryDTO getLotSummary(Long lotId, String token);                                    
	PlatformSummaryDTO getPlatformSummary(String token);                                      

    void logOccupancy(Long lotId, int occupiedSpots, int totalSpots);
}
