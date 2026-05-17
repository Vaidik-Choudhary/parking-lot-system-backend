package com.parkease.parkinglot.mapper;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.entity.ParkingLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class ParkingLotMapperTest {
    private ParkingLotMapper mapper;
    @BeforeEach void setUp() { mapper = new ParkingLotMapper(); }

    @Test void toEntity_mapsAllFields() {
        ParkingLotRequestDTO dto = new ParkingLotRequestDTO();
        dto.setName("City Mall"); dto.setAddress("MG Road"); dto.setCity("Mumbai");
        dto.setLatitude(19.0); dto.setLongitude(72.8); dto.setTotalSpots(100);
        dto.setOpenTime(LocalTime.of(8,0)); dto.setCloseTime(LocalTime.of(22,0));
        dto.setImageUrl("img.jpg");
        ParkingLot entity = mapper.toEntity(dto, "manager@test.com");
        assertEquals("City Mall", entity.getName());
        assertEquals(100, entity.getTotalSpots());
        assertEquals(100, entity.getAvailableSpots());
        assertEquals("manager@test.com", entity.getManagerEmail());
        assertFalse(entity.isOpen());
        assertFalse(entity.isApproved());
    }

    @Test void toDTO_mapsAllFields() {
        ParkingLot lot = ParkingLot.builder()
                .lotId(1L).name("City Mall").address("MG Road").city("Mumbai")
                .latitude(19.0).longitude(72.8).totalSpots(100).availableSpots(60)
                .isOpen(true).isApproved(true)
                .openTime(LocalTime.of(8,0)).closeTime(LocalTime.of(22,0)).build();
        ParkingLotResponseDTO dto = mapper.toDTO(lot);
        assertEquals(1L, dto.getLotId());
        assertEquals("Mumbai", dto.getCity());
        assertEquals(100, dto.getTotalSpots());
        assertEquals(60, dto.getAvailableSpots());
        assertTrue(dto.isOpen());
    }

    @Test void toDTOWithDistance_setsDistanceKm() {
        ParkingLot lot = ParkingLot.builder().lotId(1L).name("T").address("A")
                .city("C").totalSpots(10).availableSpots(5).build();
        ParkingLotResponseDTO dto = mapper.toDTO(lot, 3.456789);
        assertEquals(3.46, dto.getDistanceKm());
    }
}
