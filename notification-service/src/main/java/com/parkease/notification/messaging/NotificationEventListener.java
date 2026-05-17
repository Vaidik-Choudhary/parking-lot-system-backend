package com.parkease.notification.messaging;

import com.parkease.notification.config.RabbitMQConfig;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import com.parkease.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the notification.queue and delegates to NotificationService.
 * This is the only consumer â€” notification-service owns this queue.
 *
 * No changes to NotificationService were needed: we simply map the
 * incoming event onto the existing SendNotificationRequest DTO.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
    	
        // ðŸ”¥ ADD THESE LINES HERE
        log.info("EVENT FULL: {}", event);
        log.info("TYPE RAW: '{}'", event.getType());
        log.info("CHANNEL RAW: '{}'", event.getChannel());
    	
        log.info("Received notification event [{}] for: {}",
                event.getType(), event.getRecipientEmail());

        try {
            SendNotificationRequest request = mapToRequest(event);
            notificationService.send(request);
            log.debug("Notification [{}] processed successfully for: {}",
                    event.getType(), event.getRecipientEmail());
        } catch (Exception e) {
            // Throwing here will cause Spring AMQP to retry then route to DLQ
            log.error("Failed to process notification event [{}] for {}: {}",
                    event.getType(), event.getRecipientEmail(), e.getMessage());
            throw new RuntimeException("Notification processing failed", e);
        }
    }

    private SendNotificationRequest mapToRequest(NotificationEvent event) {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientEmail(event.getRecipientEmail());
        req.setTitle(event.getTitle());
        req.setMessage(event.getMessage());
        req.setRelatedId(event.getRelatedId());
        req.setRelatedType(event.getRelatedType());

        // Parse type String â†’ enum
        NotificationType type;
        try {
            type = NotificationType.valueOf(event.getType().toUpperCase().trim());
        } catch (Exception e) {
            log.warn("Unknown notification type '{}', defaulting to BROADCAST", event.getType());
            type = NotificationType.BROADCAST;
        }
        req.setType(type);

        // Channel is already a NotificationChannel enum in notification-service's event
        req.setChannel(event.getChannel() != null
                ? event.getChannel()
                : NotificationChannel.BOTH);

        return req;
    }
}
