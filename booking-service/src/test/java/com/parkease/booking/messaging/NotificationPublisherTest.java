package com.parkease.booking.messaging;

import com.parkease.booking.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks NotificationPublisher publisher;
    NotificationEvent event;

    @BeforeEach void setUp() {
        event = NotificationEvent.builder().recipientEmail("d@t.com")
                .type("BOOKING_CONFIRMED").title("Confirmed").message("Spot reserved")
                .relatedId(1L).relatedType("BOOKING").build();
    }

    @Test void publish_sendsToExchange() {
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), eq(event));
    }

    @Test void publish_rabbitFails_doesNotThrow() {
        doThrow(new RuntimeException("Broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertDoesNotThrow(() -> publisher.publish(event));
    }
}
