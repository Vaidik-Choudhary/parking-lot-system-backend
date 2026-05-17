package com.parkease.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkease.booking.dto.request.CreateSubscriptionRequest;
import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;
import com.parkease.booking.service.SubscriptionService;
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
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService service;

    @InjectMocks
    private SubscriptionController controller;

    private MockMvc mvc;
    private ObjectMapper om;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        auth = new UsernamePasswordAuthenticationToken("d@t.com", null, Collections.emptyList());
    }

    @Test
    void createRequest_Returns200() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setLotId(1L);
        req.setSpotId(10L);
        
        SubscriptionRequestResponseDTO resp = new SubscriptionRequestResponseDTO();
        resp.setId(100L);
        
        when(service.createRequest(any(), anyString())).thenReturn(resp);
        
        mvc.perform(post("/api/subscriptions/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req))
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void getMyRequests_Returns200() throws Exception {
        when(service.getDriverRequests("d@t.com")).thenReturn(List.of(new SubscriptionRequestResponseDTO()));
        mvc.perform(get("/api/subscriptions/requests/driver").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void getLotRequests_Returns200() throws Exception {
        when(service.getPendingRequestsByLot(1L)).thenReturn(List.of(new SubscriptionRequestResponseDTO()));
        mvc.perform(get("/api/subscriptions/requests/lot/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void approveRequest_Returns200() throws Exception {
        SubscriptionResponseDTO resp = new SubscriptionResponseDTO();
        resp.setId(50L);
        when(service.approveRequest(100L, "Ok")).thenReturn(resp);
        
        mvc.perform(post("/api/subscriptions/requests/100/approve")
                .param("comment", "Ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void rejectRequest_Returns200() throws Exception {
        doNothing().when(service).rejectRequest(100L, "No");
        mvc.perform(post("/api/subscriptions/requests/100/reject")
                .param("comment", "No"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyActive_Returns200() throws Exception {
        when(service.getMyActiveSubscriptions("d@t.com")).thenReturn(List.of(new SubscriptionResponseDTO()));
        mvc.perform(get("/api/subscriptions/active").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void cancelSubscription_Returns200() throws Exception {
        doNothing().when(service).cancelSubscription(50L, "d@t.com");
        mvc.perform(post("/api/subscriptions/50/cancel").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveSubscriptions_Returns200() throws Exception {
        when(service.getActiveSubscriptionsByLot(1L)).thenReturn(List.of(new SubscriptionResponseDTO()));
        mvc.perform(get("/api/subscriptions/lot/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void getSubscribedSpotIds_Returns200() throws Exception {
        when(service.getSubscribedSpotIds(1L)).thenReturn(List.of(10L));
        mvc.perform(get("/api/subscriptions/lot/1/subscribed-spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(10));
    }
}
