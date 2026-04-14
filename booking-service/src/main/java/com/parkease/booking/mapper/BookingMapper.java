package com.parkease.booking.mapper;

import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.Booking;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BookingMapper {

    public BookingResponseDTO toDTO(Booking b) {
        return BookingResponseDTO.builder()
                .bookingId(b.getBookingId())
                .driverEmail(b.getDriverEmail())
                .lotId(b.getLotId())
                .spotId(b.getSpotId())
                .vehiclePlate(b.getVehiclePlate())
                .bookingType(b.getBookingType())
                .status(b.getStatus())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .checkInTime(b.getCheckInTime())
                .checkOutTime(b.getCheckOutTime())
                .pricePerHour(b.getPricePerHour())
                .totalAmount(b.getTotalAmount())
                .estimatedAmount(calculateEstimate(b))
                .createdAt(b.getCreatedAt())
                .build();
    }

    private double calculateEstimate(Booking b) {
        if (b.getStartTime() == null || b.getEndTime() == null) return 0;
        long minutes = Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
        double hours = Math.max(1.0, minutes / 60.0);  
        return Math.round(hours * b.getPricePerHour() * 100.0) / 100.0;
    }
}
