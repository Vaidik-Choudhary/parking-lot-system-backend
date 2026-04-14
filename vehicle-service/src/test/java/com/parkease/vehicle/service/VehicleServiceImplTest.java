package com.parkease.vehicle.service;

import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.entity.Vehicle;
import com.parkease.vehicle.entity.VehicleType;
import com.parkease.vehicle.exception.ResourceNotFoundException;
import com.parkease.vehicle.mapper.VehicleMapper;
import com.parkease.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository repo;

    @Mock
    private VehicleMapper mapper;

    @InjectMocks
    private VehicleServiceImpl service;

    private Vehicle vehicle;
    private VehicleResponseDTO responseDTO;
    private VehicleRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .vehicleId(1L)
                .ownerEmail("vaidik@test.com")
                .licensePlate("MP04AB1234")
                .make("Hyundai")
                .model("i20")
                .color("White")
                .vehicleType(VehicleType.FOUR_WHEELER)
                .isEV(false)
                .isActive(true)
                .build();

        responseDTO = new VehicleResponseDTO();

        requestDTO = new VehicleRequestDTO();
        requestDTO.setLicensePlate("mp04ab1234");
        requestDTO.setMake("Hyundai");
        requestDTO.setModel("i20");
        requestDTO.setColor("White");
        requestDTO.setVehicleType(VehicleType.FOUR_WHEELER);
        requestDTO.setEV(false);
    }

    @Test
    void shouldRegisterVehicleSuccessfully() {
        when(repo.existsByOwnerEmailAndLicensePlate(
                "vaidik@test.com", "MP04AB1234"
        )).thenReturn(false);

        when(mapper.toEntity(any(VehicleRequestDTO.class), eq("vaidik@test.com")))
                .thenReturn(vehicle);
        when(repo.save(vehicle)).thenReturn(vehicle);
        when(mapper.toDTO(vehicle)).thenReturn(responseDTO);

        VehicleResponseDTO result =
                service.registerVehicle(requestDTO, "vaidik@test.com");

        assertNotNull(result);
        verify(repo).save(vehicle);
        assertEquals("MP04AB1234", requestDTO.getLicensePlate());
    }

    @Test
    void shouldThrowWhenDuplicateVehiclePlateExists() {
        when(repo.existsByOwnerEmailAndLicensePlate(
                "vaidik@test.com", "MP04AB1234"
        )).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.registerVehicle(requestDTO, "vaidik@test.com"));
    }

    @Test
    void shouldUpdateVehicleSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehicle));
        when(repo.save(vehicle)).thenReturn(vehicle);
        when(mapper.toDTO(vehicle)).thenReturn(responseDTO);

        VehicleResponseDTO result =
                service.updateVehicle(1L, requestDTO, "vaidik@test.com");

        assertNotNull(result);
        assertEquals("MP04AB1234", vehicle.getLicensePlate());
        verify(repo).save(vehicle);
    }

    @Test
    void shouldThrowWhenUpdatingVehicleOfAnotherOwner() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateVehicle(1L, requestDTO, "other@test.com"));
    }

    @Test
    void shouldDeleteVehicleSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehicle));

        service.deleteVehicle(1L, "vaidik@test.com");

        verify(repo).delete(vehicle);
    }

    @Test
    void shouldDeactivateVehicleSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehicle));

        service.deactivateVehicle(1L, "vaidik@test.com");

        assertFalse(vehicle.isActive());
        verify(repo).save(vehicle);
    }

    @Test
    void shouldGetVehicleByIdSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(vehicle));
        when(mapper.toDTO(vehicle)).thenReturn(responseDTO);

        VehicleResponseDTO result = service.getVehicleById(1L);

        assertNotNull(result);
    }

    @Test
    void shouldGetMyVehiclesSuccessfully() {
        when(repo.findByOwnerEmailAndIsActiveTrue("vaidik@test.com"))
                .thenReturn(List.of(vehicle));
        when(mapper.toDTO(vehicle)).thenReturn(responseDTO);

        List<VehicleResponseDTO> result =
                service.getMyVehicles("vaidik@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetByLicensePlateSuccessfully() {
        when(repo.findByLicensePlate("MP04AB1234"))
                .thenReturn(Optional.of(vehicle));
        when(mapper.toDTO(vehicle)).thenReturn(responseDTO);

        VehicleResponseDTO result =
                service.getByLicensePlate("mp04ab1234");

        assertNotNull(result);
    }
}