package com.parkease.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseParkingLotDTO {
    private String name;
    private String address;
    private String city;

    private int totalSpots;

    private LocalTime openTime;
    private LocalTime closeTime;

    @JsonProperty("isHandicappedFriendly")
    private boolean isHandicappedFriendly;
    @JsonProperty("hasEV")
    private boolean hasEV;
    @JsonProperty("hasTwoWheeler")
    private boolean hasTwoWheeler;
    @JsonProperty("hasFourWheeler")
    private boolean hasFourWheeler;
    @JsonProperty("hasHeavy")
    private boolean hasHeavy;

    private String imageUrl;
}
