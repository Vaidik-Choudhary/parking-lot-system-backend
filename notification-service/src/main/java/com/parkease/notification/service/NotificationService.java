package com.parkease.notification.service;

import com.parkease.notification.dto.request.BroadcastRequest;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.NotificationResponseDTO;

import java.util.List;

public interface NotificationService {

    // ── Sending ───────────────────────────────────────────────────────────────
    NotificationResponseDTO send(SendNotificationRequest request);
    int broadcast(BroadcastRequest request, List<String> recipientEmails);

    // ── Reading ───────────────────────────────────────────────────────────────
    List<NotificationResponseDTO> getMyNotifications(String email);
    List<NotificationResponseDTO> getUnread(String email);
    int getUnreadCount(String email);

    // ── State management ──────────────────────────────────────────────────────
    NotificationResponseDTO markAsRead(Long notificationId, String email);
    int markAllAsRead(String email);
    void deleteNotification(Long notificationId, String email);
}
