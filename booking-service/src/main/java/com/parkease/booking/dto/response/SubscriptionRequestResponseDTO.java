package com.parkease.booking.dto.response;

import com.parkease.booking.entity.SubscriptionRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestResponseDTO {
    private Long id;
    private String driverEmail;
    private Long lotId;
    private Long spotId;
    private SubscriptionRequestStatus status;
    private String managerComment;
    private LocalDateTime createdAt;
}
