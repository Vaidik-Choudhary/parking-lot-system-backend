package com.parkease.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.*;
import com.parkease.notification.service.NotificationService;


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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock NotificationService service;
    @InjectMocks NotificationController controller;
    MockMvc mvc;
    ObjectMapper om;
    NotificationResponseDTO resp;
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u@t.com", null, Collections.emptyList());

    @BeforeEach void setUp() {
        om = new ObjectMapper(); om.registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = NotificationResponseDTO.builder().notificationId(1L).recipientEmail("u@t.com")
                .type(NotificationType.BOOKING_CONFIRMED).channel(NotificationChannel.BOTH)
                .title("Test").message("Test msg").isRead(false).sentAt(LocalDateTime.now()).build();
    }

    @Test void send_returns201() throws Exception {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientEmail("u@t.com"); req.setType(NotificationType.BOOKING_CONFIRMED);
        req.setChannel(NotificationChannel.BOTH); req.setTitle("Test"); req.setMessage("Msg");
        when(service.send(any())).thenReturn(resp);
        mvc.perform(post("/api/notifications/send").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.notificationId").value(1));
    }

    @Test void getMyNotifications_returns200() throws Exception {
        when(service.getMyNotifications("u@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/notifications/my").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test void getUnread_returns200() throws Exception {
        when(service.getUnread("u@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/notifications/my/unread").principal(auth)).andExpect(status().isOk());
    }

    @Test void getUnreadCount_returns200() throws Exception {
        when(service.getUnreadCount("u@t.com")).thenReturn(3);
        mvc.perform(get("/api/notifications/my/count").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test void markAsRead_returns200() throws Exception {
        resp.setRead(true);
        when(service.markAsRead(1L,"u@t.com")).thenReturn(resp);
        mvc.perform(put("/api/notifications/1/read").principal(auth)).andExpect(status().isOk());
    }

    @Test void markAllRead_returns200() throws Exception {
        when(service.markAllAsRead("u@t.com")).thenReturn(5);
        mvc.perform(put("/api/notifications/read-all").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void delete_returns200() throws Exception {
        doNothing().when(service).deleteNotification(1L,"u@t.com");
        mvc.perform(delete("/api/notifications/1").principal(auth)).andExpect(status().isOk());
    }
}

