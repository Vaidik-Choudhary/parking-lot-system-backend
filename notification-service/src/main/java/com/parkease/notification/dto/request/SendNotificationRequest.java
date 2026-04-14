package com.parkease.notification.dto.request;

import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotBlank(message = "Recipient email is required")
    @Email
    private String recipientEmail;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Channel is required (IN_APP, EMAIL, BOTH)")
    private NotificationChannel channel;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Long relatedId;
    private String relatedType;  
}
