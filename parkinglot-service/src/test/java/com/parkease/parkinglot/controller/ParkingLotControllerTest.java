package com.parkease.parkinglot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkease.parkinglot.dto.request.ParkingLotRequestDTO;
import com.parkease.parkinglot.dto.response.ParkingLotResponseDTO;
import com.parkease.parkinglot.service.ParkingLotService;
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
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotControllerTest {
    @Mock ParkingLotService service;
    @InjectMocks ParkingLotController controller;
    MockMvc mvc;
    ObjectMapper om;
    ParkingLotResponseDTO resp;
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("mgr@t.com", null, Collections.emptyList());

    @BeforeEach void setUp() {
        om = new ObjectMapper(); om.registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = ParkingLotResponseDTO.builder().lotId(1L).name("Test Lot").city("Mumbai")
                .totalSpots(50).availableSpots(30).isOpen(true).isApproved(true).build();
    }

    @Test void getAllOpenLots_returns200() throws Exception {
        when(service.getOpenLots()).thenReturn(List.of(resp));
        mvc.perform(get("/api/lots")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotId").value(1));
    }

    @Test void getLotById_returns200() throws Exception {
        when(service.getLotById(1L)).thenReturn(resp);
        mvc.perform(get("/api/lots/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Lot"));
    }

    @Test void getLotsByCity_returns200() throws Exception {
        when(service.getByCity(anyString(), any(), any(), any(), any(), any())).thenReturn(List.of(resp));
        mvc.perform(get("/api/lots/city/Mumbai")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Mumbai"));
    }

    @Test void getNearbyLots_returns200() throws Exception {
        when(service.getNearbyLots(any(com.parkease.parkinglot.dto.request.NearbySearchRequest.class))).thenReturn(List.of(resp));
        mvc.perform(get("/api/lots/nearby").param("lat","19.0").param("lon","72.8").param("radius","5.0"))
                .andExpect(status().isOk());
    }

    @Test void createLot_returns201() throws Exception {
        ParkingLotRequestDTO req = new ParkingLotRequestDTO();
        req.setName("Test"); req.setAddress("Addr"); req.setCity("Mumbai"); req.setTotalSpots(50);
        req.setLatitude(19.0); req.setLongitude(72.8);
        when(service.createLot(any(), anyString())).thenReturn(resp);
        mvc.perform(post("/api/lots").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isCreated());
    }

    @Test void updateLot_returns200() throws Exception {
        ParkingLotRequestDTO req = new ParkingLotRequestDTO();
        req.setName("Updated"); req.setAddress("A"); req.setCity("B"); req.setTotalSpots(10);
        req.setLatitude(19.0); req.setLongitude(72.8);
        when(service.updateLot(anyLong(), any(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/lots/1").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isOk());
    }

    @Test void deleteLot_returns200() throws Exception {
        doNothing().when(service).deleteLot(anyLong(), anyString());
        mvc.perform(delete("/api/lots/1").principal(auth)).andExpect(status().isOk());
    }

    @Test void approveLot_returns200() throws Exception {
        when(service.approveLot(1L)).thenReturn(resp);
        mvc.perform(put("/api/lots/admin/1/approve")).andExpect(status().isOk());
    }

    @Test void rejectLot_returns200() throws Exception {
        when(service.rejectLot(1L)).thenReturn(resp);
        mvc.perform(put("/api/lots/admin/1/reject")).andExpect(status().isOk());
    }

    @Test void toggleOpen_returns200() throws Exception {
        when(service.toggleOpen(anyLong(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/lots/1/toggle").principal(auth)).andExpect(status().isOk());
    }

}
