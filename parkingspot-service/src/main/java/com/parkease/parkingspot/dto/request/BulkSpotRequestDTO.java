package com.parkease.parkingspot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BulkSpotRequestDTO extends BaseSpotRequestDTO {

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 200, message = "Cannot create more than 200 spots at once")
    private int count;

    @NotBlank(message = "Prefix is required (e.g. A, B, GF)")
    private String prefix;
}
