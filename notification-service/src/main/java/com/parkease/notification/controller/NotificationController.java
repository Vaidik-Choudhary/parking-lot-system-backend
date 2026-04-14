package com.parkease.notification.controller;

import com.parkease.notification.dto.request.BroadcastRequest;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.ApiResponse;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponseDTO> send(
            @Valid @RequestBody SendNotificationRequest request) {
        log.info("POST /api/notifications/send → [{}] to {}",
                request.getType(), request.getRecipientEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.send(request));
    }


    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(
            Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("GET /api/notifications/my - user: {}", email);
        return ResponseEntity.ok(service.getMyNotifications(email));
    }

    /**
     * GET /api/notifications/my/unread
     * Returns only unread notifications.
     */
    @GetMapping("/my/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnread(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getUnread(email));
    }

    /**
     * GET /api/notifications/my/count
     * Returns the unread count — used for the notification bell badge.
     * Response: { "unreadCount": 3 }
     */
    @GetMapping("/my/count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(Authentication auth) {
        String email = (String) auth.getPrincipal();
        int count = service.getUnreadCount(email);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a single notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/notifications/{}/read - user: {}", id, email);
        return ResponseEntity.ok(service.markAsRead(id, email));
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read (clears the badge count).
     * Response: { "success": true, "message": "5 notifications marked as read." }
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("PUT /api/notifications/read-all - user: {}", email);
        int count = service.markAllAsRead(email);
        return ResponseEntity.ok(ApiResponse.ok(count + " notifications marked as read."));
    }

    /**
     * DELETE /api/notifications/{id}
     * Delete a notification from the inbox.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id, Authentication auth) {
        String email = (String) auth.getPrincipal();
        log.info("DELETE /api/notifications/{} - user: {}", id, email);
        service.deleteNotification(id, email);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted."));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> broadcast(
            @Valid @RequestBody BroadcastRequest request,
            @RequestParam List<String> emails) {
        log.info("POST /api/notifications/broadcast - target: {} recipients: {}",
                request.getTargetRole(), emails.size());
        int sent = service.broadcast(request, emails);
        return ResponseEntity.ok(ApiResponse.ok(
                "Broadcast sent to " + sent + " recipients."));
    }
}
