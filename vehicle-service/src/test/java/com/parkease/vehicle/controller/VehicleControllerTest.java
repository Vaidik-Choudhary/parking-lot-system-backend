package com.parkease.vehicle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.vehicle.dto.request.VehicleRequestDTO;
import com.parkease.vehicle.dto.response.VehicleResponseDTO;
import com.parkease.vehicle.entity.VehicleType;
import com.parkease.vehicle.service.VehicleService;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {
    @Mock VehicleService service;
    @InjectMocks VehicleController controller;
    MockMvc mvc;
    ObjectMapper om = new ObjectMapper();
    VehicleResponseDTO resp;
    VehicleRequestDTO req;
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("d@t.com", null, Collections.emptyList());

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = VehicleResponseDTO.builder().vehicleId(1L).ownerEmail("d@t.com")
                .licensePlate("MP04AB1234").make("Hyundai").model("i20")
                .vehicleType(VehicleType.FOUR_WHEELER).build();
        req = new VehicleRequestDTO();
        req.setLicensePlate("MP04AB1234"); req.setMake("Hyundai"); req.setModel("i20");
        req.setColor("White"); req.setVehicleType(VehicleType.FOUR_WHEELER);
    }

    @Test void register_returns201() throws Exception {
        when(service.registerVehicle(any(), anyString())).thenReturn(resp);
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vehicleId").value(1));
    }

    @Test void getMyVehicles_returns200() throws Exception {
        when(service.getMyVehicles("d@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/vehicles/my").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(1));
    }

    @Test void getById_returns200() throws Exception {
        when(service.getVehicleById(1L)).thenReturn(resp);
        mvc.perform(get("/api/vehicles/1")).andExpect(status().isOk()).andExpect(jsonPath("$.vehicleId").value(1));
    }

    @Test void getByPlate_returns200() throws Exception {
        when(service.getByLicensePlate("MP04AB1234")).thenReturn(resp);
        mvc.perform(get("/api/vehicles/plate/MP04AB1234")).andExpect(status().isOk());
    }

    @Test void update_returns200() throws Exception {
        when(service.updateVehicle(anyLong(), any(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/vehicles/1").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth)).andExpect(status().isOk());
    }

    @Test void delete_returns200() throws Exception {
        doNothing().when(service).deleteVehicle(anyLong(), anyString());
        mvc.perform(delete("/api/vehicles/1").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void deactivate_returns200() throws Exception {
        doNothing().when(service).deactivateVehicle(anyLong(), anyString());
        mvc.perform(put("/api/vehicles/1/deactivate").principal(auth)).andExpect(status().isOk());
    }
}
