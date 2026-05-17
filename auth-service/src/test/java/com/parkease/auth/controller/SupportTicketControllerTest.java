package com.parkease.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.dto.request.SupportTicketRequest;
import com.parkease.auth.entity.SupportTicket;
import com.parkease.auth.mapper.SupportTicketMapper;
import com.parkease.auth.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketControllerTest {

    @Mock private SupportTicketRepository repository;
    @Mock private SupportTicketMapper mapper;
    @InjectMocks private SupportTicketController controller;

    private MockMvc mvc;
    private ObjectMapper om = new ObjectMapper();
    private SupportTicket ticket;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        ticket = SupportTicket.builder()
                .ticketId(1L)
                .userId("user@test.com")
                .userName("Test User")
                .userRole("DRIVER")
                .subject("Test Subject")
                .description("Test Description")
                .status(SupportTicket.TicketStatus.OPEN)
                .build();
    }

    @Test
    void createTicket_shouldReturn200() throws Exception {
        SupportTicketRequest req = SupportTicketRequest.builder()
                .userId("user@test.com")
                .userName("Test User")
                .userRole("DRIVER")
                .subject("Test Subject")
                .description("Test Description")
                .build();

        when(mapper.toEntity(any())).thenReturn(ticket);
        when(repository.save(any())).thenReturn(ticket);

        mvc.perform(post("/api/support/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(1));
    }

    @Test
    void getMyTickets_shouldReturn200() throws Exception {
        when(repository.findByUserIdOrderByCreatedAtDesc("user@test.com"))
                .thenReturn(Collections.singletonList(ticket));

        mvc.perform(get("/api/support/my-tickets").param("userId", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user@test.com"));
    }

    @Test
    void getAllTickets_shouldReturn200() throws Exception {
        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Collections.singletonList(ticket));

        mvc.perform(get("/api/support/admin/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(1));
    }

    @Test
    void updateStatus_shouldReturn200() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(any())).thenReturn(ticket);

        mvc.perform(patch("/api/support/admin/tickets/1/status")
                .param("status", "RESOLVED")
                .param("notes", "Fixed"))
                .andExpect(status().isOk());
        
        verify(repository).save(any());
    }
}
