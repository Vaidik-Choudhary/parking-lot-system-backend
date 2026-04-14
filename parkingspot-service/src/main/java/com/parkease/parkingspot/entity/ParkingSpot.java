package com.parkease.parkingspot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "spot_number"}))
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spotId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "spot_number", nullable = false)
    private String spotNumber;

    @Column(nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean isEVCharging = false;

    @Column(nullable = false)
    private boolean isHandicapped = false;

    @Column(nullable = false)
    private double pricePerHour;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = SpotStatus.AVAILABLE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
