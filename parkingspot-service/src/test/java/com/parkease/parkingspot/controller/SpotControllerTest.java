package com.parkease.parkingspot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.parkingspot.dto.request.*;
import com.parkease.parkingspot.dto.response.SpotResponseDTO;
import com.parkease.parkingspot.entity.*;
import com.parkease.parkingspot.service.SpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SpotControllerTest {
    @Mock SpotService service;
    @InjectMocks SpotController controller;
    MockMvc mvc;
    ObjectMapper om = new ObjectMapper();
    SpotResponseDTO resp;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = SpotResponseDTO.builder().spotId(1L).lotId(10L).spotNumber("A1").floor(1)
                .spotType(SpotType.STANDARD).vehicleType(VehicleType.FOUR_WHEELER)
                .status(SpotStatus.AVAILABLE).pricePerHour(50.0).build();
    }

    @Test void getById_returns200() throws Exception {
        when(service.getSpotById(1L)).thenReturn(resp);
        mvc.perform(get("/api/spots/1")).andExpect(status().isOk()).andExpect(jsonPath("$.spotId").value(1));
    }

    @Test void getByLot_returns200() throws Exception {
        when(service.getSpotsByLot(10L)).thenReturn(List.of(resp));
        mvc.perform(get("/api/spots/lot/10")).andExpect(status().isOk()).andExpect(jsonPath("$[0].lotId").value(10));
    }

    @Test void getAvailable_returns200() throws Exception {
        when(service.getAvailableSpots(10L)).thenReturn(List.of(resp));
        mvc.perform(get("/api/spots/lot/10/available")).andExpect(status().isOk());
    }

    @Test void getByFloor_returns200() throws Exception {
        when(service.getSpotsByFloor(10L,1)).thenReturn(List.of(resp));
        mvc.perform(get("/api/spots/lot/10/floor/1")).andExpect(status().isOk());
    }

    @Test void countAvailable_returns200() throws Exception {
        when(service.countAvailableSpots(10L)).thenReturn(5);
        mvc.perform(get("/api/spots/lot/10/count")).andExpect(status().isOk()).andExpect(content().string("5"));
    }

    @Test void addSpot_returns201() throws Exception {
        SpotRequestDTO req = new SpotRequestDTO();
        req.setLotId(10L); req.setSpotNumber("A1"); req.setFloor(1);
        req.setSpotType(SpotType.STANDARD); req.setVehicleType(VehicleType.FOUR_WHEELER); req.setPricePerHour(50.0);
        when(service.addSpot(any())).thenReturn(resp);
        mvc.perform(post("/api/spots").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test void addBulk_returns201() throws Exception {
        BulkSpotRequestDTO req = new BulkSpotRequestDTO();
        req.setLotId(10L); req.setCount(2); req.setPrefix("A"); req.setFloor(1);
        req.setSpotType(SpotType.STANDARD); req.setVehicleType(VehicleType.FOUR_WHEELER); req.setPricePerHour(50.0);
        when(service.addBulkSpots(any())).thenReturn(List.of(resp, resp));
        mvc.perform(post("/api/spots/bulk").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test void deleteSpot_returns200() throws Exception {
        doNothing().when(service).deleteSpot(1L);
        mvc.perform(delete("/api/spots/1")).andExpect(status().isOk());
    }

    @Test void reserveSpot_returns200() throws Exception {
        when(service.reserveSpot(1L)).thenReturn(resp);
        mvc.perform(put("/api/spots/1/reserve")).andExpect(status().isOk());
    }

    @Test void occupySpot_returns200() throws Exception {
        when(service.occupySpot(1L)).thenReturn(resp);
        mvc.perform(put("/api/spots/1/occupy")).andExpect(status().isOk());
    }

    @Test void releaseSpot_returns200() throws Exception {
        when(service.releaseSpot(1L)).thenReturn(resp);
        mvc.perform(put("/api/spots/1/release")).andExpect(status().isOk());
    }

    @Test void setMaintenance_returns200() throws Exception {
        when(service.setMaintenance(1L)).thenReturn(resp);
        mvc.perform(put("/api/spots/1/maintenance")).andExpect(status().isOk());
    }
}
