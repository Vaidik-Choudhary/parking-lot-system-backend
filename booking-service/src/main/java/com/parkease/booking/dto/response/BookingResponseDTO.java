package com.parkease.booking.dto.response;

import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.entity.BookingType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder 
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {

    private Long bookingId;
    private String driverEmail;
    private Long lotId;
    private Long spotId;
    private String vehiclePlate;
    private BookingType bookingType;
    private BookingStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private double pricePerHour;
    private double totalAmount;     
    private double estimatedAmount;  

    private LocalDateTime createdAt;
}
