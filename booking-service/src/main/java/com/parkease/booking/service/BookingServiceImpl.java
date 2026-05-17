package com.parkease.booking.service;

import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.client.PaymentServiceClient;
import com.parkease.booking.client.VehicleServiceClient;
import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.dto.response.DriveInSpotDTO;
import com.parkease.booking.dto.response.ManagerDashboardDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.exception.ResourceNotFoundException;
import com.parkease.booking.mapper.BookingMapper;
import com.parkease.booking.messaging.NotificationEvent;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.SubscriptionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final String TIMEZONE = "Asia/Kolkata";
    private static final String BOOKING_ID_KEY = "bookingId";
    private static final String STATUS_OCCUPIED = "OCCUPIED";
    private static final String STATUS_MAINTENANCE = "MAINTENANCE";


    private final BookingRepository     repo;
    private final BookingMapper         mapper;
    private final SpotServiceClient     spotServiceClient;
    private final LotServiceClient      lotServiceClient;
    private final PaymentServiceClient  paymentServiceClient;
    private final VehicleServiceClient  vehicleServiceClient;
    private final NotificationPublisher notificationPublisher;
    private final SubscriptionRepository subscriptionRepository;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @Value("${app.booking.checkin-grace-minutes}")
    private int graceMinutes;

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Booking getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private void verifyDriver(Booking b, String email) {
        if (!b.getDriverEmail().equals(email))
            throw new ResourceNotFoundException("Booking not found: " + b.getBookingId());
    }

    // â”€â”€ createBooking â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Creates a new booking.
     *
     * PRE_BOOKING path:
     *  1. Validate startTime is in the future.
     *  2. Check overlap via isSpotBookedInWindow.
     *  3. Save with status = RESERVED.
     *  4. Decrement lot capacity.
     *  5. Publish BOOKING_CONFIRMED notification.
     *
     * DRIVE_IN path:
     *  1. Override startTime â†’ now (driver is already on-site).
     *  2. Check overlap (protects against racing pre-bookings).
     *  3. Save with status = ACTIVE, checkInTime = now (auto-activate).
     *  4. Mark spot OCCUPIED via spot-service.
     *  5. Decrement lot capacity.
     *  6. Publish CHECKIN notification (skip the separate check-in step).
     */
    @Override
    @Transactional
    public BookingResponseDTO createBooking(CreateBookingRequest req, String driverEmail) {
        log.info("Creating {} booking for driver={} spot={}", req.getBookingType(), driverEmail, req.getSpotId());

        // ── Pending Payment Guard ────────────────────────────────────────────────────────────────
        String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader != null) {
            try {
                long pendingCount = paymentServiceClient.getPendingCount(authHeader);
                log.info("Pending payment check for {}: count={}", driverEmail, pendingCount);
                if (pendingCount > 1) {
                    throw new BookingException("You have " + pendingCount + " pending payment(s). " +
                            "Please clear your outstanding dues before making a new booking.");
                }
            } catch (BookingException e) {
                throw e; // Re-throw our own exception
            } catch (Exception e) {
                log.error("Failed to verify pending payments from payment-service for {}: {}", driverEmail, e.getMessage());
                throw new BookingException("Unable to verify your payment status. Please try again later or contact support.");
            }
        } else {
            log.warn("No Authorization header found for driver {}. Skipping pending payment check.", driverEmail);
        }

        boolean isDriveIn = req.getBookingType() == BookingType.DRIVE_IN;
        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalDateTime();
        String normalizedPlate = req.getVehiclePlate().toUpperCase().trim();

        // ── Determine effective startTime ──────────────────────────────────────
        LocalDateTime startTime;
        if (isDriveIn) {
            startTime = now;
        } else {
            if (req.getStartTime() == null) {
                throw new IllegalArgumentException("Start time is required for PRE_BOOKING.");
            }
            if (!req.getStartTime().isAfter(now)) {
                throw new IllegalArgumentException("Start time must be in the future for PRE_BOOKING.");
            }
            startTime = req.getStartTime();
        }

        // ── Vehicle Registration Validation ────────────────────────────────────
        // A booking cannot be created without a registered vehicle.
        Map<String, Object> vehicle = fetchRegisteredVehicle(authHeader, normalizedPlate);

        // ── Vehicle-to-Spot Compatibility Validation ───────────────────────────
        // Fetch spot details and validate vehicle type compatibility.
        Map<String, Object> spotDetails = fetchSpotDetails(req.getSpotId());
        validateVehicleSpotCompatibility(vehicle, spotDetails);

        // ── Lot Operating Hours Validation ─────────────────────────────────────
        validateLotOperatingHours(req.getLotId(), isDriveIn ? "Drive-in" : "Booking");

        if (isDriveIn) {
            if (repo.existsBySpotIdAndStatus(req.getSpotId(), BookingStatus.ACTIVE)) {
                throw new BookingException("Spot " + req.getSpotId() + " is currently occupied.");
            }
            if (repo.existsByVehiclePlateAndStatus(normalizedPlate, BookingStatus.ACTIVE)) {
                throw new BookingException("Vehicle " + normalizedPlate + " is already checked in.");
            }
        }

        // ── Validate endTime ───────────────────────────────────────────────────
        if (req.getEndTime() == null) {
            throw new IllegalArgumentException("End time is required.");
        }
        if (!req.getEndTime().isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        // ── Double-booking guard (overlap check) ─────────────────────────────
        // Overlap: existing.start < requested.end AND existing.end > requested.start
        boolean conflict = repo.isSpotBookedInWindow(req.getSpotId(), startTime, req.getEndTime());
        if (conflict) {
            throw new BookingException("Spot " + req.getSpotId()
                    + " is already booked for this time window.");
        }

        boolean vehicleConflict = repo.isVehicleBookedInWindow(normalizedPlate, startTime, req.getEndTime());
        if (vehicleConflict) {
            throw new BookingException("Vehicle " + normalizedPlate
                    + " is already booked for an overlapping time window.");
        }

        // ── Monthly Subscription guard ───────────────────────────────────────
        boolean subConflict = !subscriptionRepository.findOverlappingSubscriptions(req.getSpotId(), startTime, req.getEndTime()).isEmpty();
        if (subConflict) {
            throw new BookingException("Spot " + req.getSpotId()
                    + " is reserved for a monthly subscription during this window.");
        }

        double pricePerHour = getSpotPrice(req.getSpotId());

        // ── Determine initial status & checkInTime ────────────────────────────────────────────────
        BookingStatus initialStatus = isDriveIn ? BookingStatus.ACTIVE : BookingStatus.RESERVED;
        LocalDateTime checkInTime   = isDriveIn ? now : null;

        Booking booking = Booking.builder()
                .driverEmail(driverEmail)
                .lotId(req.getLotId())
                .spotId(req.getSpotId())
                .vehiclePlate(req.getVehiclePlate().toUpperCase().trim())
                .bookingType(req.getBookingType())
                .status(initialStatus)
                .startTime(startTime)
                .endTime(req.getEndTime())
                .pricePerHour(pricePerHour)
                .totalAmount(0.0)
                .checkInTime(checkInTime)
                .build();

        Booking saved = repo.save(booking);
        log.info("Booking #{} created (type={} status={})", saved.getBookingId(), saved.getBookingType(), saved.getStatus());

        // ── Side effects ─────────────────────────────────────────────────────────────────────────
        callLotService(() -> lotServiceClient.decrementAvailable(req.getLotId()), "decrement", req.getLotId());

        if (isDriveIn) {
            // Auto-occupy the spot immediately for drive-in
            callSpotService(() -> spotServiceClient.occupySpot(req.getSpotId()), "occupy", req.getSpotId());

            notificationPublisher.publish(NotificationEvent.builder()
                    .recipientEmail(driverEmail)
                    .type("CHECKIN")
                    .title("Drive-In Booking Active!")
                    .message("You are now checked in at spot " + req.getSpotId()
                            + " (Booking #" + saved.getBookingId() + "). "
                            + "Estimated checkout: " + req.getEndTime() + ".")
                    .relatedId(saved.getBookingId())
                    .relatedType("BOOKING")
                    .build());
        } else {
            // Pre-booking: reserve the spot (not yet occupied)
            callSpotService(() -> spotServiceClient.reserveSpot(req.getSpotId()), "reserve", req.getSpotId());

            notificationPublisher.publish(NotificationEvent.builder()
                    .recipientEmail(driverEmail)
                    .type("BOOKING_CONFIRMED")
                    .title("Booking Confirmed!")
                    .message("Booking #" + saved.getBookingId()
                            + " for spot " + req.getSpotId()
                            + " confirmed from " + startTime + " to " + req.getEndTime() + ".")
                    .relatedId(saved.getBookingId())
                    .relatedType("BOOKING")
                    .build());
        }

        return mapper.toDTO(saved);
    }

    // ── checkIn ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Checks in a PRE_BOOKING within the grace window.
     * DRIVE_IN bookings are auto-activated at creation ─ calling this on a
     * DRIVE_IN booking throws a BookingException.
     */
    @Override
    @Transactional
    public BookingResponseDTO checkIn(Long bookingId, String driverEmail) {
        log.info("Check-in booking={} driver={}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot check in. Status is: " + booking.getStatus()
                    + (booking.getBookingType() == BookingType.DRIVE_IN
                        ? " ─ DRIVE_IN bookings are auto-activated at creation." : "."));
        }

        // ── Lot Operating Hours Validation ─────────────────────────────────────
        validateLotOperatingHours(booking.getLotId(), "Check-in");

        // Use IST for time comparisons — booking times are stored in IST
        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalDateTime();
        if (now.isBefore(booking.getStartTime())) {
            throw new BookingException("Too early to check in. Booking starts at " + booking.getStartTime());
        }
        LocalDateTime graceCutoff = booking.getStartTime().plusMinutes(graceMinutes);
        if (now.isAfter(graceCutoff)) {
            throw new BookingException("Check-in grace period expired. Was valid until " + graceCutoff);
        }

        // Prevent check-in if the spot is physically occupied or the vehicle is already checked in
        if (repo.existsBySpotIdAndStatus(booking.getSpotId(), BookingStatus.ACTIVE)) {
            throw new BookingException("Spot " + booking.getSpotId() + " is currently occupied by another active booking. Please wait for checkout.");
        }
        if (repo.existsByVehiclePlateAndStatus(booking.getVehiclePlate(), BookingStatus.ACTIVE)) {
            throw new BookingException("Vehicle " + booking.getVehiclePlate() + " is already checked in on another booking. Please check out first.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(now);
        Booking saved = repo.save(booking);

        callSpotService(() -> spotServiceClient.occupySpot(booking.getSpotId()), "occupy", booking.getSpotId());

        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(driverEmail)
                .type("CHECKIN")
                .title("Checked In Successfully")
                .message("Checked in for booking #" + bookingId + " at " + saved.getCheckInTime() + ".")
                .relatedId(bookingId).relatedType("BOOKING").build());

        return mapper.toDTO(saved);
    }

    // ── checkOut ─────────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponseDTO checkOut(Long bookingId, String driverEmail) {
        log.info("Check-out booking={} driver={}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BookingException("Cannot check out. Status is: " + booking.getStatus());
        }

        // ── Lot Operating Hours Validation ─────────────────────────────────────
        validateLotOperatingHours(booking.getLotId(), "Check-out");

        LocalDateTime checkOutTime = java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalDateTime();
        booking.setCheckOutTime(checkOutTime);
        booking.setStatus(BookingStatus.COMPLETED);

        double fare = computeFare(booking.getCheckInTime(), checkOutTime, booking.getPricePerHour());
        booking.setTotalAmount(fare);

        Booking saved = repo.save(booking);
        log.info("Booking #{} checked out. Fare: Rs.{}", bookingId, fare);

        callSpotService(() -> spotServiceClient.releaseSpot(booking.getSpotId()), "release", booking.getSpotId());
        callLotService(() -> lotServiceClient.incrementAvailable(booking.getLotId()), "increment", booking.getLotId());

        try {
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null) {
                java.util.Map<String, Object> paymentReq = new java.util.HashMap<>();
                paymentReq.put(BOOKING_ID_KEY, saved.getBookingId());
                paymentReq.put("amount", fare);
                paymentReq.put("description", "Parking fee for booking #" + saved.getBookingId());
                paymentServiceClient.initializePayment(authHeader, paymentReq);
            }
        } catch (Exception e) {
            log.error("Payment initialization failed for booking {}: {}", saved.getBookingId(), e.getMessage());
        }


        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(driverEmail)
                .type("CHECKOUT")
                .title("Check-Out Complete")
                .message("Checked out from booking #" + bookingId
                        + ". Total fare: Rs." + fare + ". Thank you for using ParkEase!")
                .relatedId(bookingId).relatedType("BOOKING").build());

        return mapper.toDTO(saved);
    }

    // ── cancelBooking ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId, String driverEmail) {
        log.info("Cancel booking={} driver={}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Only RESERVED bookings can be cancelled. Status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = repo.save(booking);

        callSpotService(() -> spotServiceClient.releaseSpot(booking.getSpotId()), "release", booking.getSpotId());
        callLotService(() -> lotServiceClient.incrementAvailable(booking.getLotId()), "increment", booking.getLotId());

        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(driverEmail)
                .type("CANCELLATION")
                .title("Booking Cancelled")
                .message("Booking #" + bookingId + " for spot " + booking.getSpotId() + " cancelled.")
                .relatedId(bookingId).relatedType("BOOKING").build());

        return mapper.toDTO(saved);
    }

    // ── extendBooking ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponseDTO extendBooking(Long bookingId, ExtendBookingRequest req, String driverEmail) {
        log.info("Extend booking={} driver={}", bookingId, driverEmail);

        Booking booking = getOrThrow(bookingId);
        verifyDriver(booking, driverEmail);

        if (booking.getStatus() != BookingStatus.ACTIVE && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BookingException("Cannot extend booking in status: " + booking.getStatus());
        }
        if (!req.getNewEndTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("New end time must be after current end time: " + booking.getEndTime());
        }

        // ── Lot Operating Hours Validation ─────────────────────────────────────
        validateLotOperatingHours(booking.getLotId(), "Extension");

        // Check that the extension window is conflict-free
        boolean conflict = repo.isSpotBookedInWindow(booking.getSpotId(), booking.getEndTime(), req.getNewEndTime());
        if (conflict) {
            throw new BookingException("Spot is already booked during the extension period.");
        }

        boolean vehicleConflict = repo.isVehicleBookedInWindow(booking.getVehiclePlate(), booking.getEndTime(), req.getNewEndTime());
        if (vehicleConflict) {
            throw new BookingException("Vehicle is already booked during the extension period.");
        }

        booking.setEndTime(req.getNewEndTime());
        Booking saved = repo.save(booking);

        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(driverEmail)
                .type("BOOKING_CONFIRMED")
                .title("Booking Extended")
                .message("Booking #" + bookingId + " extended. New end time: " + req.getNewEndTime() + ".")
                .relatedId(bookingId).relatedType("BOOKING").build());

        return mapper.toDTO(saved);
    }

    // ── Slot Discovery: Pre-Booking ──────────────────────────────────────────────────────────────

    /**
     * Returns spots in the lot that are free during [startTime, endTime].
     *
     * Algorithm:
     *  1. Ask booking-service DB for spot IDs already booked in the window.
     *  2. Fetch all spots for the lot from spot-service.
     *  3. Remove spots whose ID appears in the booked set OR whose physical
     *     status is OCCUPIED / MAINTENANCE (can't be booked regardless).
     *
     * This is the BookMyShow-style "only show seats not already taken" logic.
     */
    @Override
    public List<Map<String, Object>> getAvailableSpotsForPreBooking(
            Long lotId, LocalDateTime startTime, LocalDateTime endTime) {

        // Validate time range
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Both startTime and endTime are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }
        if (!startTime.isAfter(java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalDateTime())) {
            throw new IllegalArgumentException("startTime must be in the future for PRE_BOOKING.");
        }

        // Step 1: spot IDs already claimed in this window
        List<Long> bookedIds = repo.findBookedSpotIdsInWindow(lotId, startTime, endTime);
        Set<Long>  bookedSet = new HashSet<>(bookedIds);

        // Step 2: all spots for the lot
        List<Map<String, Object>> allSpots = fetchSpotsByLot(lotId);

        // Step 3: filter
        return allSpots.stream()
                .filter(spot -> {
                    Long   spotId = toLong(spot.get("spotId"));
                    String status = (String) spot.get("status");
                    
                    // Filter out already booked hourly spots
                    if (bookedSet.contains(spotId)) return false;
                    
                    // Filter out physically occupied or maintenance spots
                    if (STATUS_OCCUPIED.equals(status) || STATUS_MAINTENANCE.equals(status)) return false;
                    
                    // Filter out monthly subscriptions overlapping this window
                    return subscriptionRepository.findOverlappingSubscriptions(spotId, startTime, endTime).isEmpty();
                })
                .toList();
    }

    // ── Slot Discovery: Drive-In ─────────────────────────────────────────────────────────────────

    /**
     * Builds the real-time spot grid for Drive-In users.
     *
     * For each spot:
     *  - OCCUPIED / MAINTENANCE ─ not selectable
     *  - Has a future RESERVED booking ─ RESERVED_AVAILABLE, label "Available until HH:mm"
     *  - No future bookings ─ FREE, label "Available Now"
     *
     * This is analogous to how BookMyShow shows "available" / "booked" seats,
     * with the addition of the time-until-reservation label.
     */
    @Override
    public List<DriveInSpotDTO> getDriveInSpotView(Long lotId) {
        LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalDateTime();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<Map<String, Object>> allSpots = fetchSpotsByLot(lotId);

        return allSpots.stream().map(spot -> {
            Long   spotId = toLong(spot.get("spotId"));
            String status = (String) spot.get("status");

            // Base fields shared across all states
            DriveInSpotDTO.DriveInSpotDTOBuilder builder = DriveInSpotDTO.builder()
                    .spotId(spotId)
                    .spotNumber((String) spot.get("spotNumber"))
                    .floor(toInt(spot.get("floor")))
                    .spotType(toStr(spot.get("spotType")))
                    .vehicleType(toStr(spot.get("vehicleType")))
                    .pricePerHour(toDouble(spot.get("pricePerHour")))
                    .isEVCharging(toBool(spot.get("isEVCharging")))
                    .isHandicapped(toBool(spot.get("isHandicapped")));

            // Physically occupied or under maintenance ─ not selectable
            if (STATUS_OCCUPIED.equals(status)) {
                return builder.status(STATUS_OCCUPIED)
                        .selectable(false)
                        .availabilityLabel("Currently Occupied")
                        .build();
            }
            if (STATUS_MAINTENANCE.equals(status)) {
                return builder.status(STATUS_MAINTENANCE)
                        .selectable(false)
                        .availabilityLabel("Under Maintenance")
                        .build();
            }

            // Find the nearest upcoming PRE_BOOKING reservation for this spot
            List<Booking> reservations = repo.findActiveOrReservedBookingsForSpot(spotId);
            
            // Monthly check: is it currently reserved for a subscription?
            boolean hasActiveSub = !subscriptionRepository.findOverlappingSubscriptions(spotId, now, now.plusMinutes(30)).isEmpty();
            if (hasActiveSub) {
                return builder.status(STATUS_OCCUPIED)
                        .selectable(false)
                        .availabilityLabel("Monthly Subscription")
                        .build();
            }

            // ── CRITICAL FIX ─────────────────────────────────────────────────────────────────────
            // 1. If there's an ACTIVE booking, the car is parked.
            // 2. If there's a RESERVED booking that has already started (startTime <= now)
            //    but hasn't expired yet (driver is within grace period), it's also occupied.
            boolean isCurrentlyOccupiedOrReserved = reservations.stream()
                    .anyMatch(b -> b.getStatus() == BookingStatus.ACTIVE ||
                            (b.getStatus() == BookingStatus.RESERVED && !b.getStartTime().isAfter(now)));

            if (isCurrentlyOccupiedOrReserved) {
                return builder.status(STATUS_OCCUPIED)
                        .selectable(false)
                        .availabilityLabel("Currently Occupied")
                        .build();
            }
            // ─────────────────────────────────────────────────────────────────────────────────────

            Optional<Booking> nextReservation = reservations.stream()
                    .filter(b -> b.getStatus() == BookingStatus.RESERVED && b.getStartTime().isAfter(now))
                    .min(Comparator.comparing(Booking::getStartTime));

            if (nextReservation.isPresent()) {
                LocalDateTime reservedFrom = nextReservation.get().getStartTime();
                // Drive-in user CAN use this slot but must leave before the reservation starts
                return builder.status("RESERVED_AVAILABLE")
                        .selectable(true)
                        .availabilityLabel("Available until " + reservedFrom.format(timeFmt))
                        .reservedFrom(reservedFrom)
                        .build();
            }

            // Fully free slot
            return builder.status("FREE")
                    .selectable(true)
                    .availabilityLabel("Available Now")
                    .build();

        }).toList();
    }

    // ── Query operations ─────────────────────────────────────────────────────────────────────────

    @Override
    public BookingResponseDTO getBookingById(Long bookingId) {
        BookingResponseDTO dto = mapper.toDTO(getOrThrow(bookingId));
        
        try {
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null) {
                Map<String, Object> payment = paymentServiceClient.getPaymentByBookingId(authHeader, bookingId);
                if (payment != null) {
                    Object status = payment.get("status");
                    dto.setPaid(status != null && String.valueOf(status).equalsIgnoreCase("PAID"));
                }
            }
        } catch (Exception e) {
            log.warn("Payment status not found for booking {}: {}", bookingId, e.getMessage());
        }

        return dto;
    }

    @Override
    public List<BookingResponseDTO> getMyBookings(String driverEmail) {
        List<BookingResponseDTO> bookings = repo.findByDriverEmailOrderByCreatedAtDesc(driverEmail)
                .stream().map(mapper::toDTO).toList();

        // Populate isPaid by checking payment-service
        try {
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null) {
                List<Map<String, Object>> payments = paymentServiceClient.getMyPayments(authHeader);
                log.info("DEBUG: Raw payments for {}: {}", driverEmail, payments);
                
                Set<Long> paidBookingIds = payments.stream()
                        .filter(p -> {
                            Object status = p.get("status");
                            return status != null && String.valueOf(status).equalsIgnoreCase("PAID");
                        })
                        .filter(p -> p.get(BOOKING_ID_KEY) != null) // skip subscription payments
                        .map(p -> ((Number) p.get(BOOKING_ID_KEY)).longValue())
                        .collect(Collectors.toSet());

                log.info("Found {} paid booking IDs: {}", paidBookingIds.size(), paidBookingIds);
                bookings.forEach(b -> b.setPaid(paidBookingIds.contains(b.getBookingId())));
            } else {
                log.warn("No Authorization header found in request, skipping payment status check");
            }
        } catch (Exception e) {
            log.error("Could not fetch payment status for bookings: {}", e.getMessage(), e);
        }

        return bookings;
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
    public List<Long> getDistinctLotIds() {
        return repo.findDistinctLotIds();
    }

    @Override
    public double calculateFare(Long bookingId) {
        Booking b = getOrThrow(bookingId);
        if (b.getCheckInTime() == null) throw new BookingException("Driver has not checked in yet.");
        LocalDateTime end = b.getCheckOutTime() != null ? b.getCheckOutTime() : LocalDateTime.now();
        return computeFare(b.getCheckInTime(), end, b.getPricePerHour());
    }

    // ── Manager Dashboard ────────────────────────────────────────────────────────────────────────

    /**
     * Active bookings for a lot: drivers currently parked (status = ACTIVE).
     * The manager sees this in the "Active Bookings" notification panel.
     */
    @Override
    public List<BookingResponseDTO> getActiveBookingsByLot(Long lotId) {
        return repo.findActiveBookingsByLot(lotId)
                .stream().map(mapper::toDTO).toList();
    }

    /**
     * Upcoming bookings for a lot: reservations whose startTime > now.
     * The manager sees this in the "Upcoming Bookings" notification panel.
     */
    @Override
    public List<BookingResponseDTO> getUpcomingBookingsByLot(Long lotId) {
        return repo.findUpcomingBookingsByLot(lotId, LocalDateTime.now())
                .stream().map(mapper::toDTO).toList();
    }

    /**
     * Aggregated dashboard combining active + upcoming counts and lists.
     */
    @Override
    public ManagerDashboardDTO getManagerDashboard(Long lotId) {
        List<BookingResponseDTO> active   = getActiveBookingsByLot(lotId);
        List<BookingResponseDTO> upcoming = getUpcomingBookingsByLot(lotId);
        return ManagerDashboardDTO.builder()
                .lotId(lotId)
                .totalActive(active.size())
                .totalUpcoming(upcoming.size())
                .activeBookings(active)
                .upcomingBookings(upcoming)
                .build();
    }

    // ── Vehicle Registration & Compatibility Helpers ─────────────────────

    /**
     * Fetches the registered vehicle from vehicle-service by plate number.
     * Throws BookingException if the vehicle is not found or not registered.
     */
    private Map<String, Object> fetchRegisteredVehicle(String authHeader, String plate) {
        try {
            Map<String, Object> vehicle = vehicleServiceClient.getVehicleByPlate(authHeader, plate);
            if (vehicle == null || vehicle.isEmpty()) {
                throw new BookingException(
                        "Vehicle " + plate + " is not registered. Please register your vehicle before booking.");
            }
            Boolean isActive = toBool(vehicle.get("active"));
            if (!isActive) {
                throw new BookingException(
                        "Vehicle " + plate + " is deactivated. Please reactivate it or use a different vehicle.");
            }
            return vehicle;
        } catch (BookingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch vehicle details for plate {}: {}", plate, e.getMessage());
            throw new BookingException(
                    "Vehicle " + plate + " is not registered. Please register your vehicle before booking.");
        }
    }

    /**
     * Fetches spot details from spot-service.
     */
    private Map<String, Object> fetchSpotDetails(Long spotId) {
        try {
            Map<String, Object> spot = spotServiceClient.getSpotById(spotId);
            if (spot == null || spot.isEmpty()) {
                throw new BookingException("Parking spot " + spotId + " not found.");
            }
            return spot;
        } catch (BookingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch spot details for spot {}: {}", spotId, e.getMessage());
            throw new BookingException("Unable to verify parking spot details. Please try again.");
        }
    }

    /**
     * Validates vehicle type is compatible with the parking spot type.
     *
     * Mapping:
     *   STANDARD  -> FOUR_WHEELER only
     *   MOTORBIKE -> TWO_WHEELER only
     *   LARGE     -> HEAVY only
     *   EV        -> Any vehicle type, but must have isEV = true
     */
    private void validateVehicleSpotCompatibility(Map<String, Object> vehicle, Map<String, Object> spot) {
        String spotType = toStr(spot.get("spotType"));
        String vehicleType = toStr(vehicle.get("vehicleType"));
        boolean isEV = toBool(vehicle.get("ev"));

        log.info("Compatibility check: spotType={}, vehicleType={}, isEV={}", spotType, vehicleType, isEV);

        if (spotType == null || vehicleType == null) {
            throw new BookingException("Unable to determine spot or vehicle type for compatibility check.");
        }

        switch (spotType) {
            case "EV":
                if (!isEV) {
                    throw new BookingException("Only EV vehicles are allowed in EV spots. " +
                            "Your vehicle is not marked as electric.");
                }
                if (!"FOUR_WHEELER".equals(vehicleType)) {
                    throw new BookingException("EV spots are currently for 4-wheelers only. " +
                            "Your vehicle type: " + vehicleType + ".");
                }
                break;
            case "STANDARD":
                if (!"FOUR_WHEELER".equals(vehicleType)) {
                    throw new BookingException(
                            "Selected vehicle is not compatible with this parking spot. " +
                            "STANDARD spots are for 4-wheelers only. Your vehicle type: " + vehicleType + ".");
                }
                break;
            case "MOTORBIKE":
                if (!"TWO_WHEELER".equals(vehicleType)) {
                    throw new BookingException(
                            "Selected vehicle is not compatible with this parking spot. " +
                            "MOTORBIKE spots are for 2-wheelers only. Your vehicle type: " + vehicleType + ".");
                }
                break;
            case "LARGE":
                if (!"HEAVY".equals(vehicleType)) {
                    throw new BookingException(
                            "Heavy vehicles can only be parked in LARGE spots. " +
                            "This LARGE spot requires a heavy vehicle. Your vehicle type: " + vehicleType + ".");
                }
                break;
            default:
                throw new BookingException("Unknown spot type: " + spotType + ". Cannot validate compatibility.");
        }
    }

    private void validateLotOperatingHours(Long lotId, String action) {
        try {
            Map<String, Object> lot = lotServiceClient.getLotById(lotId);
            if (lot == null || lot.isEmpty()) return;

            // Lombok @Getter on boolean isOpen generates isOpen() which Jackson serializes as "open"
            Object openFlag = lot.get("open") != null ? lot.get("open") : lot.get("isOpen");
            Boolean isOpenManually = toBool(openFlag);
            if (isOpenManually != null && !isOpenManually) {
                throw new BookingException("This parking lot is currently closed by the manager.");
            }

            Object openTimeObj = lot.get("openTime");
            Object closeTimeObj = lot.get("closeTime");

            if (openTimeObj == null || closeTimeObj == null) return;

            LocalTime openTime = LocalTime.parse(openTimeObj.toString());
            LocalTime closeTime = LocalTime.parse(closeTimeObj.toString());
            
            // Always compare against IST (Asia/Kolkata) — lot hours are in local time
            LocalTime actionTime = java.time.ZonedDateTime.now(java.time.ZoneId.of(TIMEZONE)).toLocalTime();

            // Handle cases where closeTime is on the next day (e.g., 22:00 to 06:00)
            boolean isWithinHours;
            if (closeTime.isAfter(openTime)) {
                // Normal case: 08:00 to 22:00
                isWithinHours = !actionTime.isBefore(openTime) && !actionTime.isAfter(closeTime);
            } else {
                // Overnight case: 22:00 to 06:00
                isWithinHours = !actionTime.isBefore(openTime) || !actionTime.isAfter(closeTime);
            }

            if (!isWithinHours) {
                throw new BookingException(action + " is not allowed at this time. Lot operating hours are " 
                        + openTime + " to " + closeTime + ".");
            }
        } catch (BookingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate lot operating hours for lot {}: {}", lotId, e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────────────────────

    /** Actual fare: minimum 1 hour, rounded to 2 decimal places. */
    private double computeFare(LocalDateTime checkIn, LocalDateTime checkOut, double pricePerHour) {
        long   minutes = Duration.between(checkIn, checkOut).toMinutes();
        double hours   = Math.max(1.0, minutes / 60.0);
        return Math.round(hours * pricePerHour * 100.0) / 100.0;
    }

    /** Fetches spot price from spot-service; falls back to Rs. 50/hr on failure. */
    private double getSpotPrice(Long spotId) {
        try {
            Map<String, Object> spot = spotServiceClient.getSpotById(spotId);
            if (spot != null && spot.containsKey("pricePerHour")) {
                return ((Number) spot.get("pricePerHour")).doubleValue();
            }
        } catch (Exception e) {
            log.error("Could not fetch spot price for spot {}: {}", spotId, e.getMessage());
        }
        log.warn("Using default price Rs.50 for spot {}", spotId);
        return 50.0;
    }

    /** Fetches all spots for a lot; returns empty list on failure. */
    private List<Map<String, Object>> fetchSpotsByLot(Long lotId) {
        try {
            List<Map<String, Object>> spots = spotServiceClient.getSpotsByLot(lotId);
            return spots != null ? spots : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not fetch spots for lot {}: {}", lotId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void callSpotService(Runnable call, String action, Long spotId) {
        try {
            call.run();
        } catch (Exception e) {
            log.error("spot-service [{}] failed for spot {}: {}", action, spotId, e.getMessage());
        }
    }

    private void callLotService(Runnable call, String action, Long lotId) {
        try {
            call.run();
        } catch (Exception e) {
            log.error("lot-service [{}] failed for lot {}: {}", action, lotId, e.getMessage());
        }
    }

    // â”€â”€ Type-safe map accessors â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Long    toLong  (Object v) { return v == null ? null : ((Number) v).longValue(); }
    private int     toInt   (Object v) { return v == null ? 0    : ((Number) v).intValue(); }
    private double  toDouble(Object v) { return v == null ? 0.0  : ((Number) v).doubleValue(); }
    private String  toStr   (Object v) { return v == null ? null : v.toString(); }
    private boolean toBool  (Object v) { return v instanceof Boolean b && b; }
}
