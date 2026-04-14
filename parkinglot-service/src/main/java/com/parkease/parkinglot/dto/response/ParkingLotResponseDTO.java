package com.parkease.parkinglot.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotResponseDTO {

    private Long lotId;
    private String name;
    private String address;
    private String city;

    private double latitude;
    private double longitude;

    private int totalSpots;
    private int availableSpots;

    private Long managerId;

    private boolean isOpen;
    private boolean isApproved;

    private LocalTime openTime;
    private LocalTime closeTime;

    private String imageUrl;

    private LocalDateTime createdAt;

    private Double distanceKm;
}
