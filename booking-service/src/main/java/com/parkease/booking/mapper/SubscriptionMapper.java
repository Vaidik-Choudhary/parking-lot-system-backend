package com.parkease.booking.mapper;

import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;
import com.parkease.booking.entity.Subscription;
import com.parkease.booking.entity.SubscriptionRequest;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionRequestResponseDTO toRequestDTO(SubscriptionRequest request) {
        return SubscriptionRequestResponseDTO.builder()
                .id(request.getId())
                .driverEmail(request.getDriverEmail())
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .status(request.getStatus())
                .managerComment(request.getManagerComment())
                .createdAt(request.getCreatedAt())
                .build();
    }

    public SubscriptionResponseDTO toDTO(Subscription subscription) {
        return SubscriptionResponseDTO.builder()
                .id(subscription.getId())
                .driverEmail(subscription.getDriverEmail())
                .lotId(subscription.getLotId())
                .spotId(subscription.getSpotId())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
