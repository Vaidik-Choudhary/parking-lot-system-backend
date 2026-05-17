package com.parkease.booking.controller;

import com.parkease.booking.dto.request.CreateSubscriptionRequest;
import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;
import com.parkease.booking.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping("/request")
    public ResponseEntity<SubscriptionRequestResponseDTO> createRequest(
            @Valid @RequestBody CreateSubscriptionRequest request,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.createRequest(request, email));
    }

    @GetMapping("/requests/driver")
    public ResponseEntity<List<SubscriptionRequestResponseDTO>> getMyRequests(
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getDriverRequests(email));
    }


    @GetMapping("/requests/lot/{lotId}")
    public ResponseEntity<List<SubscriptionRequestResponseDTO>> getLotRequests(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getPendingRequestsByLot(lotId));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<SubscriptionResponseDTO> approveRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(service.approveRequest(id, comment));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        service.rejectRequest(id, comment);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMyActive(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getMyActiveSubscriptions(email));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable Long id,
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        service.cancelSubscription(id, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/lot/{lotId}/active")
    public ResponseEntity<List<SubscriptionResponseDTO>> getActiveSubscriptions(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getActiveSubscriptionsByLot(lotId));
    }

    @GetMapping("/lot/{lotId}/subscribed-spots")
    public ResponseEntity<List<Long>> getSubscribedSpotIds(@PathVariable Long lotId) {
        return ResponseEntity.ok(service.getSubscribedSpotIds(lotId));
    }
}
