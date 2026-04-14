package com.parkease.notification.service;

import com.parkease.notification.dto.request.BroadcastRequest;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.Notification;
import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.exception.ResourceNotFoundException;
import com.parkease.notification.mapper.NotificationMapper;
import com.parkease.notification.repository.NotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;
    private final NotificationMapper     mapper;
    private final EmailService           emailService;

    private Notification getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + id));
    }

    @Override
    @Transactional
    public NotificationResponseDTO send(SendNotificationRequest request) {
        log.info("Sending [{}] notification to: {} via {}",
                request.getType(), request.getRecipientEmail(), request.getChannel());

        Notification saved = null;

        if (request.getChannel() == NotificationChannel.IN_APP
                || request.getChannel() == NotificationChannel.BOTH) {

            Notification notification = Notification.builder()
                    .recipientEmail(request.getRecipientEmail())
                    .type(request.getType())
                    .channel(request.getChannel())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .relatedId(request.getRelatedId())
                    .relatedType(request.getRelatedType())
                    .isRead(false)
                    .build();

            saved = repo.save(notification);
            log.debug("In-app notification saved with id: {}", saved.getNotificationId());
        }

        if (request.getChannel() == NotificationChannel.EMAIL
                || request.getChannel() == NotificationChannel.BOTH) {

            emailService.sendEmail(
                    request.getRecipientEmail(),
                    request.getTitle(),
                    request.getTitle(),
                    request.getMessage()
            );
        }

        if (saved != null) {
            return mapper.toDTO(saved);
        }

        return NotificationResponseDTO.builder()
                .recipientEmail(request.getRecipientEmail())
                .type(request.getType())
                .channel(request.getChannel())
                .title(request.getTitle())
                .message(request.getMessage())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int broadcast(BroadcastRequest request, List<String> recipientEmails) {
        log.info("Broadcasting to {} recipients (target role: {})",
                recipientEmails.size(), request.getTargetRole());

        if (recipientEmails.isEmpty()) {
            log.warn("Broadcast called with empty recipient list");
            return 0;
        }

        List<Notification> notifications = recipientEmails.stream()
                .map(email -> Notification.builder()
                        .recipientEmail(email)
                        .type(com.parkease.notification.entity.NotificationType.BROADCAST)
                        .channel(NotificationChannel.IN_APP)
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .isRead(false)
                        .build())
                .toList();

        repo.saveAll(notifications);

        log.info("Broadcast saved for {} recipients", notifications.size());
        return notifications.size();
    }

    @Override
    public List<NotificationResponseDTO> getMyNotifications(String email) {
        log.debug("Fetching all notifications for: {}", email);
        return repo.findByRecipientEmailOrderBySentAtDesc(email)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<NotificationResponseDTO> getUnread(String email) {
        return repo.findByRecipientEmailAndIsReadFalseOrderBySentAtDesc(email)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public int getUnreadCount(String email) {
        return repo.countByRecipientEmailAndIsReadFalse(email);
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId, String email) {
        Notification n = getOrThrow(notificationId);

        if (!n.getRecipientEmail().equals(email)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            n = repo.save(n);
            log.debug("Notification {} marked as read by {}", notificationId, email);
        }

        return mapper.toDTO(n);
    }

    @Override
    @Transactional
    public int markAllAsRead(String email) {
        int count = repo.markAllAsRead(email);
        log.info("Marked {} notifications as read for: {}", count, email);
        return count;
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, String email) {
        Notification n = getOrThrow(notificationId);

        if (!n.getRecipientEmail().equals(email)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        repo.delete(n);
        log.debug("Notification {} deleted by {}", notificationId, email);
    }
}
