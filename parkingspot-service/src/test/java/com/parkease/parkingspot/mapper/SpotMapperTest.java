package com.parkease.parkingspot.mapper;

import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpotMapperTest {
    private SpotMapper mapper;
    @BeforeEach void setUp() { mapper = new SpotMapper(); }

    @Test void toEntity_mapsAllFields() {
        SpotRequestDTO dto = new SpotRequestDTO();
        dto.setLotId(10L); dto.setSpotNumber("A1"); dto.setFloor(1);
        dto.setSpotType(SpotType.STANDARD); dto.setVehicleType(VehicleType.FOUR_WHEELER);
        dto.setEVCharging(true); dto.setHandicapped(false); dto.setPricePerHour(50.0);
        ParkingSpot entity = mapper.toEntity(dto);
        assertEquals(10L, entity.getLotId());
        assertEquals("A1", entity.getSpotNumber());
        assertEquals(SpotStatus.AVAILABLE, entity.getStatus());
        assertTrue(entity.isEVCharging());
        assertEquals(50.0, entity.getPricePerHour());
    }

    @Test void toDTO_mapsAllFields() {
        ParkingSpot spot = ParkingSpot.builder()
                .spotId(1L).lotId(10L).spotNumber("B2").floor(2)
                .spotType(SpotType.STANDARD).vehicleType(VehicleType.TWO_WHEELER)
                .status(SpotStatus.OCCUPIED).isEVCharging(false).isHandicapped(true)
                .pricePerHour(40.0).build();
        SpotResponseDTO dto = mapper.toDTO(spot);
        assertEquals(1L, dto.getSpotId());
        assertEquals(SpotStatus.OCCUPIED, dto.getStatus());
        assertTrue(dto.isHandicapped());
        assertEquals(40.0, dto.getPricePerHour());
    }
}
