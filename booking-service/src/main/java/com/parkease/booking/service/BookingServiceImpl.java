package com.parkease.booking.service;

import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.exception.ResourceNotFoundException;
import com.parkease.booking.mapper.BookingMapper;
import com.parkease.booking.repository.BookingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repo;
    private final BookingMapper     mapper;
    private final RestTemplate      restTemplate;

    @Value("${app.services.spot-service}")
    private String spotServiceUrl;

    @Value("${app.services.parkinglot-service}")
    private String lotServiceUrl;

    @Value("${app.booking.checkin-grace-minutes}")
    private int graceMinutes;

    private Booking getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private void verifyDriver(Booking b, String email) {
        if (!b.getDriverEmail().equals(email)) {
            throw new ResourceNotFoundException("Booking not found with id: " + b.getBookingId());
        }
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(CreateBookingRequest req, String driverEmail) {
        log.info("Creating booking for driver: {} on spot: {}", driverEmail, req.getSpotId());

        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        boolean alreadyBooked = repo.isSpotBookedInWindow(
                req.getSpotId(), req.getStartTime(), req.getEndTime());
        if (alreadyBooked) {
            throw new BookingException("Spot " + req.getSpotId()
                    + " is already booked for this time window.");
        }

        double pricePerHour = getSpotPrice(req.getSpotId());

        Booking booking = Booking.builder()
                .driverEmail(driverEmail)
                .lotId(req.getLotId())
                .spotId(req.getSpotId())
                .vehiclePlate(req.getVehiclePlate().toUpperCase().trim())
                .bookingType(req.getBookingType())
                .status(BookingStatus.RESERVED)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .pricePerHour(pricePerHour)
                .totalAmount(0.0) 
                .build();

        Booking saved = repo.save(booking);
        log.info("Booking created with id: {}", saved.getBookingId());

        callSpotService(req.getSpotId(), "reserve");

        callLotService(req.getLotId(), "decrement");

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public BookingResponseDTO checkIn(Long bookingId, String driverEmail) {
        log.info("Check-in for booking: {} driver: {}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot check in. Booking status is: " + booking.getStatus());
        }

        if (booking.getBookingType() == BookingType.PRE_BOOKING) {
            LocalDateTime graceCutoff = booking.getStartTime().plusMinutes(graceMinutes);
            if (LocalDateTime.now().isAfter(graceCutoff)) {
                throw new BookingException(
                    "Check-in grace period expired. Your booking was for "
                    + booking.getStartTime() + " with " + graceMinutes + " minutes grace.");
            }
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now());
        Booking saved = repo.save(booking);

        callSpotService(booking.getSpotId(), "occupy");

        log.info("Booking {} checked in at {}", bookingId, saved.getCheckInTime());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public BookingResponseDTO checkOut(Long bookingId, String driverEmail) {
        log.info("Check-out for booking: {} driver: {}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BookingException("Cannot check out. Booking status is: " + booking.getStatus());
        }

        LocalDateTime checkOutTime = LocalDateTime.now();
        booking.setCheckOutTime(checkOutTime);
        booking.setStatus(BookingStatus.COMPLETED);

        double fare = computeFare(booking.getCheckInTime(), checkOutTime, booking.getPricePerHour());
        booking.setTotalAmount(fare);

        Booking saved = repo.save(booking);
        log.info("Booking {} checked out. Fare: ₹{}", bookingId, fare);

        callSpotService(booking.getSpotId(), "release");

        callLotService(booking.getLotId(), "increment");

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId, String driverEmail) {
        log.info("Cancelling booking: {} driver: {}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException(
                "Cannot cancel booking in status: " + booking.getStatus()
                + ". Only RESERVED bookings can be cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = repo.save(booking);

        callSpotService(booking.getSpotId(), "release");

        callLotService(booking.getLotId(), "increment");

        log.info("Booking {} cancelled", bookingId);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public BookingResponseDTO extendBooking(Long bookingId, ExtendBookingRequest req, String driverEmail) {
        log.info("Extending booking: {} driver: {}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.ACTIVE
                && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot extend booking in status: " + booking.getStatus());
        }

        if (!req.getNewEndTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("New end time must be after current end time: "
                    + booking.getEndTime());
        }

        boolean conflict = repo.isSpotBookedInWindow(
                booking.getSpotId(), booking.getEndTime(), req.getNewEndTime());
        if (conflict) {
            throw new BookingException("Spot is already booked during the extension period.");
        }

        booking.setEndTime(req.getNewEndTime());
        Booking saved = repo.save(booking);
        log.info("Booking {} extended to {}", bookingId, req.getNewEndTime());
        return mapper.toDTO(saved);
    }

    @Override
    public BookingResponseDTO getBookingById(Long bookingId) {
        return mapper.toDTO(getOrThrow(bookingId));
    }

    @Override
    public List<BookingResponseDTO> getMyBookings(String driverEmail) {
        return repo.findByDriverEmailOrderByCreatedAtDesc(driverEmail)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<BookingResponseDTO> getActiveBookings(String driverEmail) {
        return repo.findByDriverEmailAndStatus(driverEmail, BookingStatus.ACTIVE)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<BookingResponseDTO> getBookingsByLot(Long lotId) {
        return repo.findByLotIdOrderByCreatedAtDesc(lotId)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<BookingResponseDTO> getAllBookings() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public double calculateFare(Long bookingId) {
        Booking b = getOrThrow(bookingId);
        if (b.getCheckInTime() == null) {
            throw new BookingException("Driver has not checked in yet.");
        }
        LocalDateTime end = b.getCheckOutTime() != null ? b.getCheckOutTime() : LocalDateTime.now();
        return computeFare(b.getCheckInTime(), end, b.getPricePerHour());
    }

    private double computeFare(LocalDateTime checkIn, LocalDateTime checkOut, double pricePerHour) {
        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        double hours = Math.max(1.0, minutes / 60.0); 
        return Math.round(hours * pricePerHour * 100.0) / 100.0;
    }

    private void callSpotService(Long spotId, String action) {
        String url = spotServiceUrl + "/api/spots/" + spotId + "/" + action;
        try {
            restTemplate.put(url, null);
            log.debug("Spot-service called: {} for spot {}", action, spotId);
        } catch (Exception e) {
            log.error("Failed to call spot-service [{}] for spot {}: {}", action, spotId, e.getMessage());
        }
    }

  
    private void callLotService(Long lotId, String action) {
        String url = lotServiceUrl + "/api/lots/" + lotId + "/" + action;
        try {
            restTemplate.put(url, null);
            log.debug("Lot-service called: {} for lot {}", action, lotId);
        } catch (Exception e) {
            log.error("Failed to call lot-service [{}] for lot {}: {}", action, lotId, e.getMessage());
        }
    }


    @SuppressWarnings("unchecked")
    private double getSpotPrice(Long spotId) {
        try {
            String url = spotServiceUrl + "/api/spots/" + spotId;
            Map<String, Object> spot = restTemplate.getForObject(url, Map.class);
            if (spot != null && spot.containsKey("pricePerHour")) {
                return ((Number) spot.get("pricePerHour")).doubleValue();
            }
        } catch (Exception e) {
            log.error("Could not fetch spot price for spot {}: {}", spotId, e.getMessage());
        }
        log.warn("Using default price 50.0 for spot {}", spotId);
        return 50.0;
    }
}
