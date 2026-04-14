package com.parkease.notification.mapper;

import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDTO toDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .notificationId(n.getNotificationId())
                .recipientEmail(n.getRecipientEmail())
                .type(n.getType())
                .channel(n.getChannel())
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.isRead())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .build();
    }
}
