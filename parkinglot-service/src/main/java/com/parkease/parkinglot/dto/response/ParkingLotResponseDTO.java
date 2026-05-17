package com.parkease.parkinglot.dto.response;

import com.parkease.parkinglot.dto.BaseParkingLotDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ParkingLotResponseDTO extends BaseParkingLotDTO {

    private Long lotId;
    private double latitude;
    private double longitude;

    private int availableSpots;

    private Long managerId;

    private boolean isOpen;
    private boolean isApproved;

    private LocalDateTime createdAt;

    private Double distanceKm;
}
