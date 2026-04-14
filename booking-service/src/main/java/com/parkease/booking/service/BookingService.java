package com.parkease.booking.service;

import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;

import java.util.List;


public interface BookingService {

    // ── Driver operations ─────────────────────────────────────────────────────
    BookingResponseDTO createBooking(CreateBookingRequest request, String driverEmail);
    BookingResponseDTO checkIn(Long bookingId, String driverEmail);
    BookingResponseDTO checkOut(Long bookingId, String driverEmail);
    BookingResponseDTO cancelBooking(Long bookingId, String driverEmail);
    BookingResponseDTO extendBooking(Long bookingId, ExtendBookingRequest request, String driverEmail);

    // ── Query operations ──────────────────────────────────────────────────────
    BookingResponseDTO getBookingById(Long bookingId);
    List<BookingResponseDTO> getMyBookings(String driverEmail);
    List<BookingResponseDTO> getActiveBookings(String driverEmail);
    List<BookingResponseDTO> getBookingsByLot(Long lotId);
    List<BookingResponseDTO> getAllBookings();   // admin only

    // ── Fare calculation ──────────────────────────────────────────────────────
    double calculateFare(Long bookingId);
}
