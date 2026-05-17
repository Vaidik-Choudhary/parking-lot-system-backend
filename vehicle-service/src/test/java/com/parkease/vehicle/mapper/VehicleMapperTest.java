package com.parkease.vehicle.mapper;

import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleMapperTest {
    private VehicleMapper mapper;
    @BeforeEach void setUp() { mapper = new VehicleMapper(); }

    @Test void toEntity_mapsAllFieldsAndUppercasesPlate() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setLicensePlate("  mp04ab1234  ");
        dto.setMake("Hyundai"); dto.setModel("i20"); dto.setColor("White");
        dto.setVehicleType(VehicleType.FOUR_WHEELER); dto.setEV(true);
        Vehicle v = mapper.toEntity(dto, "owner@t.com");
        assertEquals("MP04AB1234", v.getLicensePlate());
        assertEquals("owner@t.com", v.getOwnerEmail());
        assertTrue(v.isEV());
        assertTrue(v.isActive());
    }

    @Test void toDTO_mapsAllFields() {
        Vehicle v = Vehicle.builder().vehicleId(1L).ownerEmail("o@t.com")
                .licensePlate("MP04AB1234").make("Hyundai").model("i20").color("White")
                .vehicleType(VehicleType.FOUR_WHEELER).isEV(false).isActive(true).build();
        VehicleResponseDTO dto = mapper.toDTO(v);
        assertEquals(1L, dto.getVehicleId());
        assertEquals("MP04AB1234", dto.getLicensePlate());
        assertFalse(dto.isEV());
        assertTrue(dto.isActive());
    }
}
