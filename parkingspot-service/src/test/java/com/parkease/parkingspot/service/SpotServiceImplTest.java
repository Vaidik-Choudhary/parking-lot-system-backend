package com.parkease.parkingspot.service;

import com.parkease.parkingspot.dto.request.BulkSpotRequestDTO;
import com.parkease.parkingspot.dto.request.SpotRequestDTO;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.ParkingSpot;
import com.parkease.parkingspot.entity.SpotStatus;
import com.parkease.parkingspot.entity.SpotType;
import com.parkease.parkingspot.entity.VehicleType;
import com.parkease.parkingspot.exception.SpotNotAvailableException;
import com.parkease.parkingspot.mapper.SpotMapper;
import com.parkease.parkingspot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

    @Mock
    private SpotRepository repo;

    @Mock
    private SpotMapper mapper;

    @InjectMocks
    private SpotServiceImpl service;

    private ParkingSpot spot;
    private SpotRequestDTO requestDTO;
    private SpotResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        spot = ParkingSpot.builder()
                .spotId(1L)
                .lotId(10L)
                .spotNumber("A1-01")
                .floor(1)
                .spotType(SpotType.COMPACT)
                .vehicleType(VehicleType.FOUR_WHEELER)
                .status(SpotStatus.AVAILABLE)
                .pricePerHour(50.0)
                .build();

        requestDTO = new SpotRequestDTO();
        requestDTO.setLotId(10L);
        requestDTO.setSpotNumber("A1-01");
        requestDTO.setFloor(1);
        requestDTO.setSpotType(SpotType.COMPACT);
        requestDTO.setVehicleType(VehicleType.FOUR_WHEELER);
        requestDTO.setPricePerHour(50.0);

        responseDTO = new SpotResponseDTO();
    }

    @Test
    void shouldAddSpotSuccessfully() {
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-01")).thenReturn(false);
        when(mapper.toEntity(requestDTO)).thenReturn(spot);
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.addSpot(requestDTO);

        assertNotNull(result);
        verify(repo).save(spot);
    }

    @Test
    void shouldThrowWhenDuplicateSpotExists() {
        when(repo.existsByLotIdAndSpotNumber(10L, "A1-01")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.addSpot(requestDTO));
    }

    @Test
    void shouldBulkCreateSpotsSuccessfully() {
        BulkSpotRequestDTO dto = new BulkSpotRequestDTO();
        dto.setLotId(10L);
        dto.setPrefix("A");
        dto.setFloor(1);
        dto.setCount(2);
        dto.setSpotType(SpotType.COMPACT);
        dto.setVehicleType(VehicleType.FOUR_WHEELER);
        dto.setPricePerHour(50.0);

        when(repo.existsByLotIdAndSpotNumber(anyLong(), anyString()))
                .thenReturn(false);
        when(repo.saveAll(anyList()))
                .thenReturn(List.of(
                        spot,
                        ParkingSpot.builder()
                                .spotId(2L)
                                .lotId(10L)
                                .spotNumber("A1-02")
                                .status(SpotStatus.AVAILABLE)
                                .build()
                ));
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        List<SpotResponseDTO> result = service.addBulkSpots(dto);

        assertEquals(2, result.size());
        verify(repo).saveAll(anyList());
    }

    @Test
    void shouldDeleteSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));

        service.deleteSpot(1L);

        verify(repo).delete(spot);
    }

    @Test
    void shouldThrowWhenDeletingReservedSpot() {
        spot.setStatus(SpotStatus.RESERVED);
        when(repo.findById(1L)).thenReturn(Optional.of(spot));

        assertThrows(SpotNotAvailableException.class,
                () -> service.deleteSpot(1L));
    }

    @Test
    void shouldReserveSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.reserveSpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.RESERVED, spot.getStatus());
    }

    @Test
    void shouldOccupySpotSuccessfully() {
        spot.setStatus(SpotStatus.RESERVED);

        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.occupySpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.OCCUPIED, spot.getStatus());
    }

    @Test
    void shouldReleaseSpotSuccessfully() {
        spot.setStatus(SpotStatus.OCCUPIED);

        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.releaseSpot(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.AVAILABLE, spot.getStatus());
    }

    @Test
    void shouldSetMaintenanceSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(spot));
        when(repo.save(spot)).thenReturn(spot);
        when(mapper.toDTO(spot)).thenReturn(responseDTO);

        SpotResponseDTO result = service.setMaintenance(1L);

        assertNotNull(result);
        assertEquals(SpotStatus.MAINTENANCE, spot.getStatus());
    }

    @Test
    void shouldCountAvailableSpotsSuccessfully() {
        when(repo.countByLotIdAndStatus(10L, SpotStatus.AVAILABLE))
                .thenReturn(5);

        int result = service.countAvailableSpots(10L);

        assertEquals(5, result);
    }
}