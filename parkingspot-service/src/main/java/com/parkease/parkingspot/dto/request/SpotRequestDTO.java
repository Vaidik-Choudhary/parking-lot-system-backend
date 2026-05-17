package com.parkease.parkingspot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotRequestDTO extends BaseSpotRequestDTO {

    @NotBlank(message = "Spot number is required (e.g. A-01)")
    private String spotNumber;
}
