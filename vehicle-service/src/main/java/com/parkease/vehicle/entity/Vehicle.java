package com.parkease.vehicle.entity;
 
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "vehicles", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_email", "license_plate"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;
 
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;
 
    @Column(name = "license_plate", nullable = false)
    private String licensePlate;
 
    @Column(nullable = false)
    private String make;      
 
    @Column(nullable = false)
    private String model;      
 
    private String color;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;
 
    @Column(nullable = false)
    private boolean isEV = false;
 
    @Column(nullable = false)
    private boolean isActive = true;
 
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;
 
    private LocalDateTime updatedAt;
 
    @PrePersist
    public void prePersist() {
        registeredAt = LocalDateTime.now();
        updatedAt    = LocalDateTime.now();
    }
 
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
