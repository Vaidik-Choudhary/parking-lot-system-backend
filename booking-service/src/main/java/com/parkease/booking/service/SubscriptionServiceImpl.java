package com.parkease.booking.service;

import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.client.PaymentServiceClient;
import com.parkease.booking.dto.request.CreateSubscriptionRequest;
import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.exception.ResourceNotFoundException;
import com.parkease.booking.mapper.SubscriptionMapper;
import com.parkease.booking.messaging.NotificationEvent;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.SubscriptionRequestRepository;
import com.parkease.booking.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRequestRepository requestRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final BookingRepository bookingRepo;
    private final SubscriptionMapper mapper;
    private final SpotServiceClient spotServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public SubscriptionRequestResponseDTO createRequest(CreateSubscriptionRequest request, String driverEmail) {
        log.info("Creating subscription request for spot {} by {}", request.getSpotId(), driverEmail);

        // 1. Verify spot subscription eligibility
        Map<String, Object> spot = spotServiceClient.getSpotById(request.getSpotId());
        if (spot == null || !toBool(spot.get("monthlySubscriptionEnabled"))) {
            throw new BookingException("This spot does not support monthly subscriptions.");
        }

        SubscriptionRequest subscriptionRequest = SubscriptionRequest.builder()
                .driverEmail(driverEmail)
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .status(SubscriptionRequestStatus.PENDING)
                .build();

        return mapper.toRequestDTO(requestRepo.save(subscriptionRequest));
    }

    @Override
    public List<SubscriptionRequestResponseDTO> getPendingRequestsByLot(Long lotId) {
        return requestRepo.findByLotIdAndStatus(lotId, SubscriptionRequestStatus.PENDING)
                .stream().map(mapper::toRequestDTO).toList();
    }

    @Override
    public List<SubscriptionRequestResponseDTO> getDriverRequests(String driverEmail) {
        return requestRepo.findByDriverEmailOrderByCreatedAtDesc(driverEmail)
                .stream().map(mapper::toRequestDTO).toList();
    }

    @Override
    @Transactional
    public SubscriptionResponseDTO approveRequest(Long requestId, String managerComment) {
        SubscriptionRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != SubscriptionRequestStatus.PENDING) {
            throw new BookingException("Request is already processed.");
        }

        request.setStatus(SubscriptionRequestStatus.APPROVED);
        request.setManagerComment(managerComment);
        requestRepo.save(request);

        // Delayed Start Engine logic
        LocalDateTime startDate = calculateNextAvailableDate(request.getSpotId());
        LocalDateTime endDate = startDate.plusMonths(1);

        // Fetch spot details to get monthlyRate
        Double monthlyRate = 0.0;
        try {
            Map<String, Object> spot = spotServiceClient.getSpotById(request.getSpotId());
            if (spot != null && spot.get("monthlyRate") != null) {
                monthlyRate = ((Number) spot.get("monthlyRate")).doubleValue();
            }
        } catch (Exception e) {
            log.error("Failed to fetch monthly rate for spot {} during approval: {}", request.getSpotId(), e.getMessage());
        }

        boolean isImmediate = !startDate.isAfter(LocalDateTime.now());

        Subscription subscription = Subscription.builder()
                .driverEmail(request.getDriverEmail())
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .startDate(startDate)
                .endDate(endDate)
                .monthlyRate(monthlyRate)
                .status(isImmediate ? SubscriptionStatus.ACTIVE : SubscriptionStatus.SCHEDULED)
                .build();

        Subscription saved = subscriptionRepo.save(subscription);

        // If subscription starts immediately, reserve the spot
        if (isImmediate) {
            try {
                spotServiceClient.reserveSpot(request.getSpotId());
                log.info("Spot {} reserved for subscription #{}", request.getSpotId(), saved.getId());
            } catch (Exception e) {
                log.error("Failed to reserve spot {} for subscription #{}: {}", request.getSpotId(), saved.getId(), e.getMessage());
            }
        }

        // Notify Driver
        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(request.getDriverEmail())
                .type("SUBSCRIPTION_APPROVED")
                .title("Monthly Subscription Approved!")
                .message("Your request for spot " + request.getSpotId() + " has been approved. " +
                         "Start date: " + startDate.toLocalDate())
                .relatedId(saved.getId())
                .relatedType("SUBSCRIPTION")
                .build());

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, String managerComment) {
        SubscriptionRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        request.setStatus(SubscriptionRequestStatus.REJECTED);
        request.setManagerComment(managerComment);
        requestRepo.save(request);

        // Notify Driver
        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(request.getDriverEmail())
                .type("SUBSCRIPTION_REJECTED")
                .title("Monthly Request Rejected")
                .message("Your request for spot " + request.getSpotId() + " was rejected. Comment: " + managerComment)
                .relatedId(request.getId())
                .relatedType("SUBSCRIPTION_REQUEST")
                .build());
    }

    @Override
    public List<SubscriptionResponseDTO> getActiveSubscriptionsByLot(Long lotId) {
        return subscriptionRepo.findByLotIdAndStatus(lotId, SubscriptionStatus.ACTIVE)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<SubscriptionResponseDTO> getMyActiveSubscriptions(String email) {
        return subscriptionRepo.findAll().stream()
                .filter(s -> s.getDriverEmail().equals(email) && 
                            (s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.SCHEDULED))
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<Long> getSubscribedSpotIds(Long lotId) {
        // Get spot IDs that have ACTIVE or SCHEDULED subscriptions
        List<Long> subscribedIds = subscriptionRepo.findAll().stream()
                .filter(s -> s.getLotId().equals(lotId) &&
                            (s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.SCHEDULED))
                .map(Subscription::getSpotId)
                .distinct()
                .toList();

        // Also get spot IDs with PENDING requests (to prevent duplicate requests)
        List<Long> pendingIds = requestRepo.findByLotIdAndStatus(lotId, SubscriptionRequestStatus.PENDING)
                .stream().map(SubscriptionRequest::getSpotId)
                .distinct()
                .toList();

        // Merge both lists
        java.util.Set<Long> all = new java.util.HashSet<>(subscribedIds);
        all.addAll(pendingIds);
        return new java.util.ArrayList<>(all);
    }

    @Override
    @Transactional
    public void cancelSubscription(Long id, String email) {
        Subscription sub = subscriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        if (!sub.getDriverEmail().equals(email)) {
            throw new BookingException("Access denied.");
        }
        
        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.SCHEDULED) {
            throw new BookingException("Only Active or Scheduled subscriptions can be cancelled.");
        }

        LocalDateTime now = LocalDateTime.now();
        double finalAmount = 0.0;

        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            finalAmount = calculateProratedAmount(sub, now);
            
            log.info("Subscription #{} (Active) - Pro-rated amount calculated: {}", sub.getId(), finalAmount);
            
            // Release spot
            try {
                spotServiceClient.releaseSpot(sub.getSpotId());
            } catch (Exception e) {
                log.error("Failed to release spot {} on cancellation: {}", sub.getSpotId(), e.getMessage());
            }
        } else {
            log.info("Subscription #{} (Scheduled) - Cancelled without charge.", sub.getId());
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setEndDate(now); 
        subscriptionRepo.save(sub);

        if (finalAmount > 0) {
            initializeProratedPayment(sub, finalAmount, now);
        }
        
        notificationPublisher.publish(NotificationEvent.builder()
                .recipientEmail(email)
                .type("SUBSCRIPTION_CANCELLED")
                .title("Subscription Cancelled [" + java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now()) + "]")
                .message("Your subscription for spot " + sub.getSpotId() + " has been cancelled. " +
                         (finalAmount > 0 ? "Final pro-rated charge: ₹" + String.format("%.2f", finalAmount) : "No charge applied."))
                .relatedId(sub.getId())
                .relatedType("SUBSCRIPTION")
                .build());
    }

    private double calculateProratedAmount(Subscription sub, LocalDateTime now) {
        LocalDateTime start = sub.getStartDate() != null ? sub.getStartDate() : sub.getCreatedAt();
        long daysHeld = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, now));
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, sub.getEndDate());
        if (totalDays <= 0) totalDays = 30;
        double rate = sub.getMonthlyRate() != null ? sub.getMonthlyRate() : 0.0;
        return (rate / totalDays) * daysHeld;
    }

    private void initializeProratedPayment(Subscription sub, double finalAmount, LocalDateTime now) {
        try {
            long daysHeld = java.time.temporal.ChronoUnit.DAYS.between(sub.getStartDate(), now);
            com.parkease.booking.dto.request.SubscriptionPaymentRequest paymentRequest = com.parkease.booking.dto.request.SubscriptionPaymentRequest.builder()
                    .driverEmail(sub.getDriverEmail())
                    .subscriptionId(sub.getId())
                    .amount(finalAmount)
                    .description("Pro-rated charge for subscription #" + sub.getId() + " (" + daysHeld + " days held)")
                    .build();
            log.info("Sending payment request for sub #{} amount: {}", sub.getId(), finalAmount);
            paymentServiceClient.initializeSubscriptionPayment(paymentRequest);
            log.info("Successfully sent payment request for sub #{}", sub.getId());
        } catch (Exception e) {
            log.error("Failed to trigger payment for cancelled sub #{}: {}", sub.getId(), e.getMessage());
        }
    }

    /**
     * The "Delayed Start" Algorithm:
     * Finds the earliest possible date this subscription can start.
     */
    private LocalDateTime calculateNextAvailableDate(Long spotId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Check existing subscriptions (Active or Scheduled) for this spot
        LocalDateTime maxSubscriptionEnd = subscriptionRepo.findAll().stream()
                .filter(s -> s.getSpotId().equals(spotId) && 
                            (s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.SCHEDULED))
                .map(Subscription::getEndDate)
                .max(Comparator.naturalOrder())
                .orElse(now);

        // 2. Check hourly bookings
        List<Booking> activeBookings = bookingRepo.findActiveOrReservedBookingsForSpot(spotId);
        LocalDateTime maxBooking = activeBookings.stream()
                .map(Booking::getEndTime)
                .max(Comparator.naturalOrder())
                .orElse(now);

        LocalDateTime latest = maxSubscriptionEnd.isAfter(maxBooking) ? maxSubscriptionEnd : maxBooking;

        if (latest.isBefore(now) || latest.equals(now)) {
            return now;
        } else {
            // Start the next day at 00:00 to avoid mid-day transition confusion
            return latest.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT);
        }
    }

    @Override
    @Transactional
    public void processDailyActivations() {
        log.info("Running daily subscription activations...");
        List<Subscription> scheduled = subscriptionRepo.findByStatusAndStartDateLessThanEqual(SubscriptionStatus.SCHEDULED, LocalDateTime.now());
        for (Subscription sub : scheduled) {
            log.info("Activating subscription #{} for driver {}", sub.getId(), sub.getDriverEmail());
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepo.save(sub);
        }
    }

    @Override
    @Transactional
    public void processDailyExpirations() {
        log.info("Running daily subscription expirations/billing...");
        List<Subscription> active = subscriptionRepo.findByStatusAndEndDateLessThanEqual(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        for (Subscription sub : active) {
            log.info("Expiring subscription #{} for driver {}. Triggering payment...", sub.getId(), sub.getDriverEmail());
            
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepo.save(sub);
            
            // Release spot
            try {
                spotServiceClient.releaseSpot(sub.getSpotId());
            } catch (Exception e) {
                log.error("Failed to release spot {} on expiration: {}", sub.getSpotId(), e.getMessage());
            }

            // Trigger payment using stored rate
            try {
                paymentServiceClient.initializeSubscriptionPayment(com.parkease.booking.dto.request.SubscriptionPaymentRequest.builder()
                        .driverEmail(sub.getDriverEmail())
                        .subscriptionId(sub.getId())
                        .amount(sub.getMonthlyRate())
                        .description("Monthly subscription for spot " + sub.getSpotId())
                        .build());
            } catch (Exception e) {
                log.error("Failed to initialize payment for subscription #{}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    private boolean toBool(Object v) { return v instanceof Boolean b && b; }
}
