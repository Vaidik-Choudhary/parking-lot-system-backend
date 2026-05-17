package com.parkease.parkinglot.dto.request;

import com.parkease.parkinglot.dto.BaseParkingLotDTO;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ParkingLotRequestDTO extends BaseParkingLotDTO {

    @NotBlank(message = "Lot name is required")
    @Override
    public String getName() { return super.getName(); }

    @NotBlank(message = "Address is required")
    @Override
    public String getAddress() { return super.getAddress(); }

    @NotBlank(message = "City is required")
    @Override
    public String getCity() { return super.getCity(); }

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;

    @Min(value = 1, message = "Total spots must be at least 1")
    @Override
    public int getTotalSpots() { return super.getTotalSpots(); }
}
