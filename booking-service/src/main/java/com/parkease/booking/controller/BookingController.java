package com.parkease.booking.controller;

import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.ApiResponse;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService service;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> create(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("POST /api/bookings - driver: {} spot: {}", email, request.getSpotId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createBooking(request, email));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getMyBookings(email));
    }

    @GetMapping("/my/active")
    public ResponseEntity<List<BookingResponseDTO>> getMyActive(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getActiveBookings(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getBookingById(id));
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<BookingResponseDTO> checkIn(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/bookings/{}/checkin - driver: {}", id, email);
        return ResponseEntity.ok(service.checkIn(id, email));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<BookingResponseDTO> checkOut(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/bookings/{}/checkout - driver: {}", id, email);
        return ResponseEntity.ok(service.checkOut(id, email));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancel(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/bookings/{}/cancel - driver: {}", id, email);
        return ResponseEntity.ok(service.cancelBooking(id, email));
    }

    @PutMapping("/{id}/extend")
    public ResponseEntity<BookingResponseDTO> extend(
            @PathVariable Long id,
            @Valid @RequestBody ExtendBookingRequest request,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/bookings/{}/extend - driver: {}", id, email);
        return ResponseEntity.ok(service.extendBooking(id, request, email));
    }

    @GetMapping("/{id}/fare")
    public ResponseEntity<Double> getFare(@PathVariable Long id) {
        return ResponseEntity.ok(service.calculateFare(id));
    }

    @GetMapping("/lot/{lotId}")
    @PreAuthorize("hasAnyRole('LOT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getByLot(@PathVariable Long lotId) {
        log.info("GET /api/bookings/lot/{}", lotId);
        return ResponseEntity.ok(service.getBookingsByLot(lotId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getAll() {
        log.info("GET /api/bookings/admin/all");
        return ResponseEntity.ok(service.getAllBookings());
    }
}
