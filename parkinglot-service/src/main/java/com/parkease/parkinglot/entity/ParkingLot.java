package com.parkease.parkinglot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "parking_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lotId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int totalSpots;

    @Column(nullable = false)
    private int availableSpots;

    @Column(nullable = false)
    private String managerEmail;

    @Column(nullable = false)
    private boolean isOpen;

    @Column(nullable = false)
    private boolean isApproved = false;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(name = "is_handicapped_friendly", nullable = false)
    private boolean handicappedFriendly = false;

    @Column(name = "hasev", nullable = false)
    private boolean hasEV = false;

    @Column(name = "has_two_wheeler", nullable = false)
    private boolean hasTwoWheeler = false;

    @Column(name = "has_four_wheeler", nullable = false)
    private boolean hasFourWheeler = false;

    @Column(name = "has_heavy", nullable = false)
    private boolean hasHeavy = false;

    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
