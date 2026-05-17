package com.parkease.booking.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event message published to RabbitMQ to trigger a notification.
 * Must stay structurally in sync with notification-service's NotificationEvent.
 *
 * Using plain String for type/channel so booking-service does not need
 * to import notification-service's enums.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private String recipientEmail;

    /** Maps to NotificationType enum in notification-service. */
    private String type;

    /**
     * Maps to NotificationChannel enum: IN_APP | EMAIL | BOTH.
     * Default: BOTH
     */
    @Builder.Default
    private String channel = "BOTH";

    private String title;
    private String message;
    private Long   relatedId;
    private String relatedType;
}
