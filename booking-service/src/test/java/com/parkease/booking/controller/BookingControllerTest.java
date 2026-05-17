package com.parkease.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.service.BookingService;


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
class BookingControllerTest {
    @Mock BookingService service;
    @InjectMocks BookingController controller;
    MockMvc mvc;
    ObjectMapper om;
    BookingResponseDTO resp;
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("driver@t.com", null, Collections.emptyList());

    @BeforeEach void setUp() {
        om = new ObjectMapper(); om.registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        resp = BookingResponseDTO.builder().bookingId(1L).driverEmail("driver@t.com")
                .lotId(10L).spotId(101L).vehiclePlate("MP04AB1234")
                .bookingType(BookingType.PRE_BOOKING).status(BookingStatus.RESERVED)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .pricePerHour(50.0).totalAmount(0.0).build();
    }

    @Test void createBooking_returns201() throws Exception {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L); req.setSpotId(101L); req.setVehiclePlate("MP04AB1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(2));
        when(service.createBooking(any(), anyString())).thenReturn(resp);
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(1));
    }

    @Test void getMyBookings_returns200() throws Exception {
        when(service.getMyBookings("driver@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/my").principal(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1));
    }

    @Test void getMyActive_returns200() throws Exception {
        when(service.getActiveBookings("driver@t.com")).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/my/active").principal(auth)).andExpect(status().isOk());
    }

    @Test void getById_returns200() throws Exception {
        when(service.getBookingById(1L)).thenReturn(resp);
        mvc.perform(get("/api/bookings/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1));
    }

    @Test void checkIn_returns200() throws Exception {
        resp.setStatus(BookingStatus.ACTIVE);
        when(service.checkIn(anyLong(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/bookings/1/checkin").principal(auth)).andExpect(status().isOk());
    }

    @Test void checkOut_returns200() throws Exception {
        resp.setStatus(BookingStatus.COMPLETED);
        when(service.checkOut(anyLong(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/bookings/1/checkout").principal(auth)).andExpect(status().isOk());
    }

    @Test void cancel_returns200() throws Exception {
        resp.setStatus(BookingStatus.CANCELLED);
        when(service.cancelBooking(anyLong(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/bookings/1/cancel").principal(auth)).andExpect(status().isOk());
    }

    @Test void extend_returns200() throws Exception {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(LocalDateTime.now().plusHours(4));
        when(service.extendBooking(anyLong(), any(), anyString())).thenReturn(resp);
        mvc.perform(put("/api/bookings/1/extend").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)).principal(auth)).andExpect(status().isOk());
    }

    @Test void getFare_returns200() throws Exception {
        when(service.calculateFare(1L)).thenReturn(100.0);
        mvc.perform(get("/api/bookings/1/fare")).andExpect(status().isOk())
                .andExpect(content().string("100.0"));
    }

    @Test void getAvailableForPreBooking_returns200() throws Exception {
        when(service.getAvailableSpotsForPreBooking(anyLong(), any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/bookings/slots/10/available?startTime=2026-05-01T10:00:00&endTime=2026-05-01T12:00:00"))
                .andExpect(status().isOk());
    }

    @Test void getDriveInView_returns200() throws Exception {
        when(service.getDriveInSpotView(10L)).thenReturn(List.of());
        mvc.perform(get("/api/bookings/slots/10/drive-in")).andExpect(status().isOk());
    }

    @Test void getManagerDashboard_returns200() throws Exception {
        when(service.getManagerDashboard(10L)).thenReturn(new com.parkease.booking.dto.response.ManagerDashboardDTO());
        mvc.perform(get("/api/bookings/manager/10/dashboard")).andExpect(status().isOk());
    }

    @Test void getManagerActive_returns200() throws Exception {
        when(service.getActiveBookingsByLot(10L)).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/manager/10/active")).andExpect(status().isOk());
    }

    @Test void getManagerUpcoming_returns200() throws Exception {
        when(service.getUpcomingBookingsByLot(10L)).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/manager/10/upcoming")).andExpect(status().isOk());
    }

    @Test void getByLot_returns200() throws Exception {
        when(service.getBookingsByLot(10L)).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/lot/10")).andExpect(status().isOk());
    }

    @Test void getAll_returns200() throws Exception {
        when(service.getAllBookings()).thenReturn(List.of(resp));
        mvc.perform(get("/api/bookings/admin/all")).andExpect(status().isOk());
    }

    @Test void getDistinctLotIds_returns200() throws Exception {
        when(service.getDistinctLotIds()).thenReturn(List.of(1L, 2L));
        mvc.perform(get("/api/bookings/internal/lot-ids")).andExpect(status().isOk());
    }
}
