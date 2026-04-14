package com.parkease.notification.dto.response;

import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import lombok.*;
import java.time.LocalDateTime;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class NotificationResponseDTO {

    private Long notificationId;
    private String recipientEmail;
    private NotificationType type;
    private NotificationChannel channel;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
