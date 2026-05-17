package com.parkease.notification.messaging;

import com.parkease.notification.entity.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private String recipientEmail;

    private String type;

    @Builder.Default
    private NotificationChannel channel = NotificationChannel.BOTH;

    private String title;
    private String message;
    private Long   relatedId;
    private String relatedType;
}