package com.parkease.booking.service;

import com.parkease.booking.dto.request.CreateSubscriptionRequest;
import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;

import java.util.List;

public interface SubscriptionService {
    SubscriptionRequestResponseDTO createRequest(CreateSubscriptionRequest request, String driverEmail);
    List<SubscriptionRequestResponseDTO> getPendingRequestsByLot(Long lotId);
    List<SubscriptionRequestResponseDTO> getDriverRequests(String driverEmail);
    SubscriptionResponseDTO approveRequest(Long requestId, String managerComment);
    void rejectRequest(Long requestId, String managerComment);
    List<SubscriptionResponseDTO> getActiveSubscriptionsByLot(Long lotId);
    List<SubscriptionResponseDTO> getMyActiveSubscriptions(String email);
    List<Long> getSubscribedSpotIds(Long lotId);
    void cancelSubscription(Long id, String email);
    void processDailyActivations();
    void processDailyExpirations();
}
