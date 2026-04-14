package com.parkease.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "occupancy_logs",
       indexes = {
           @Index(name = "idx_lot_timestamp", columnList = "lot_id, timestamp"),
           @Index(name = "idx_timestamp",     columnList = "timestamp")
       })
@Data
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private double occupancyRate;

    @Column(nullable = false)
    private int occupiedSpots;

    @Column(nullable = false)
    private int totalSpots;

    @Column(nullable = false)
    private int hourOfDay;

    @Column(nullable = false)
    private int dayOfWeek;
}
