package com.parkease.payment.messaging;

import com.parkease.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around RabbitTemplate.
 * Publishes NotificationEvent messages to the parkease.notifications exchange.
 * Failures are logged but never thrown â€” notification is best-effort and must
 * not roll back the payment transaction.
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.debug("Published [{}] notification event for: {}",
                    event.getType(), event.getRecipientEmail());
        } catch (Exception e) {
            log.error("Failed to publish notification event [{}] for {}: {}",
                    event.getType(), event.getRecipientEmail(), e.getMessage());
        }
    }
}
