package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.entity.ParkingLot;
import com.parkease.parkinglot.exception.UnauthorizedException;
import com.parkease.parkinglot.mapper.ParkingLotMapper;
import com.parkease.parkinglot.repository.ParkingLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceImplTest {

    @Mock
    private ParkingLotRepository repo;

    @Mock
    private ParkingLotMapper mapper;

    @InjectMocks
    private ParkingLotServiceImpl service;

    private ParkingLot lot;
    private ParkingLotResponseDTO responseDTO;
    private ParkingLotRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        lot = ParkingLot.builder()
                .lotId(1L)
                .name("City Mall Parking")
                .managerEmail("manager@test.com")
                .city("Bhopal")
                .latitude(23.2599)
                .longitude(77.4126)
                .totalSpots(100)
                .availableSpots(50)
                .isApproved(true)
                .isOpen(true)
                .build();

        responseDTO = new ParkingLotResponseDTO();

        requestDTO = new ParkingLotRequestDTO();
        requestDTO.setName("City Mall Parking");
        requestDTO.setAddress("MP Nagar");
        requestDTO.setCity("Bhopal");
        requestDTO.setLatitude(23.2599);
        requestDTO.setLongitude(77.4126);
        requestDTO.setTotalSpots(100);
        requestDTO.setOpenTime(LocalTime.of(8, 0));
        requestDTO.setCloseTime(LocalTime.of(22, 0));
        requestDTO.setImageUrl("image.jpg");
    }

    @Test
    void shouldCreateLotSuccessfully() {
        when(mapper.toEntity(requestDTO, "manager@test.com")).thenReturn(lot);
        when(repo.save(lot)).thenReturn(lot);
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result =
                service.createLot(requestDTO, "manager@test.com");

        assertNotNull(result);
        verify(repo).save(lot);
    }

    @Test
    void shouldGetLotByIdSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result = service.getLotById(1L);

        assertNotNull(result);
    }

    @Test
    void shouldGetLotsByCitySuccessfully() {
        when(repo.findByCityIgnoreCaseAndIsApprovedTrue("Bhopal"))
                .thenReturn(List.of(lot));
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        List<ParkingLotResponseDTO> result = service.getByCity("Bhopal");

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetNearbyLotsSuccessfully() {
        when(repo.findNearby(23.2599, 77.4126, 5.0))
                .thenReturn(List.of(lot));
        when(mapper.toDTO(eq(lot), anyDouble()))
                .thenReturn(responseDTO);

        List<ParkingLotResponseDTO> result =
                service.getNearbyLots(23.2599, 77.4126, 5.0);

        assertEquals(1, result.size());
    }

    @Test
    void shouldUpdateLotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));
        when(repo.save(lot)).thenReturn(lot);
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result =
                service.updateLot(1L, requestDTO, "manager@test.com");

        assertNotNull(result);
        verify(repo).save(lot);
    }

    @Test
    void shouldThrowWhenUpdatingAnotherManagersLot() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));

        assertThrows(UnauthorizedException.class,
                () -> service.updateLot(1L, requestDTO, "other@test.com"));
    }

    @Test
    void shouldDeleteLotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));

        service.deleteLot(1L, "manager@test.com");

        verify(repo).delete(lot);
    }

    @Test
    void shouldToggleLotOpenSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));
        when(repo.save(lot)).thenReturn(lot);
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result =
                service.toggleOpen(1L, "manager@test.com");

        assertNotNull(result);
        assertFalse(lot.isOpen());
    }

    @Test
    void shouldApproveLotSuccessfully() {
        lot.setApproved(false);

        when(repo.findById(1L)).thenReturn(Optional.of(lot));
        when(repo.save(lot)).thenReturn(lot);
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result = service.approveLot(1L);

        assertNotNull(result);
        assertTrue(lot.isApproved());
    }

    @Test
    void shouldRejectLotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));
        when(repo.save(lot)).thenReturn(lot);
        when(mapper.toDTO(lot)).thenReturn(responseDTO);

        ParkingLotResponseDTO result = service.rejectLot(1L);

        assertNotNull(result);
        assertFalse(lot.isApproved());
        assertFalse(lot.isOpen());
    }

    @Test
    void shouldDecrementSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));

        service.decrementSpot(1L);

        assertEquals(49, lot.getAvailableSpots());
        verify(repo).save(lot);
    }

    @Test
    void shouldIncrementSpotSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(lot));

        service.incrementSpot(1L);

        assertEquals(51, lot.getAvailableSpots());
        verify(repo).save(lot);
    }
}