package com.parkease.parkinglot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbySearchRequest {
    private double lat;
    private double lon;
    private double radiusKm;
    private Boolean hasEV;
    private Boolean has2W;
    private Boolean has4W;
    private Boolean hasHeavy;
    private Boolean hasHandicap;
}
