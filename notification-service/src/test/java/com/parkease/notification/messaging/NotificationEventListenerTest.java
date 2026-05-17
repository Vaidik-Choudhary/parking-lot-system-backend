package com.parkease.notification.messaging;

import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import com.parkease.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        when(notificationService.send(any())).thenReturn(
                NotificationResponseDTO.builder().notificationId(1L).build());
    }

    private NotificationEvent event(String type, NotificationChannel channel) {
        NotificationEvent e = new NotificationEvent();
        e.setRecipientEmail("u@t.com");
        e.setType(type);
        e.setChannel(channel);
        e.setTitle("Test");
        e.setMessage("Test message");
        e.setRelatedId(1L);
        e.setRelatedType("BOOKING");
        return e;
    }

    @Test
    void handle_bookingConfirmed_callsService() {
        listener.handleNotificationEvent(event("BOOKING_CONFIRMED", NotificationChannel.BOTH));
        verify(notificationService).send(any(SendNotificationRequest.class));
    }

    @Test
    void handle_mapsTypeCorrectly() {
        ArgumentCaptor<SendNotificationRequest> cap = ArgumentCaptor.forClass(SendNotificationRequest.class);
        listener.handleNotificationEvent(event("PAYMENT", NotificationChannel.BOTH));
        verify(notificationService).send(cap.capture());
        assertEquals(NotificationType.PAYMENT, cap.getValue().getType());
        assertEquals("u@t.com", cap.getValue().getRecipientEmail());
    }

    @Test
    void handle_lowercaseType_isCaseInsensitive() {
        ArgumentCaptor<SendNotificationRequest> cap = ArgumentCaptor.forClass(SendNotificationRequest.class);
        listener.handleNotificationEvent(event("checkout", NotificationChannel.BOTH));
        verify(notificationService).send(cap.capture());
        assertEquals(NotificationType.CHECKOUT, cap.getValue().getType());
    }

    @Test
    void handle_unknownType_defaultsToBroadcast() {
        ArgumentCaptor<SendNotificationRequest> cap = ArgumentCaptor.forClass(SendNotificationRequest.class);
        listener.handleNotificationEvent(event("TOTALLY_UNKNOWN_TYPE", NotificationChannel.BOTH));
        verify(notificationService).send(cap.capture());
        assertEquals(NotificationType.BROADCAST, cap.getValue().getType());
    }

    @Test
    void handle_nullChannel_defaultsToBoth() {
        ArgumentCaptor<SendNotificationRequest> cap = ArgumentCaptor.forClass(SendNotificationRequest.class);
        listener.handleNotificationEvent(event("CHECKIN", null));
        verify(notificationService).send(cap.capture());
        assertEquals(NotificationChannel.BOTH, cap.getValue().getChannel());
    }

    @Test
    void handle_inAppChannel_passedThrough() {
        ArgumentCaptor<SendNotificationRequest> cap = ArgumentCaptor.forClass(SendNotificationRequest.class);
        listener.handleNotificationEvent(event("CANCELLATION", NotificationChannel.IN_APP));
        verify(notificationService).send(cap.capture());
        assertEquals(NotificationChannel.IN_APP, cap.getValue().getChannel());
    }

    @Test
    void handle_serviceThrows_rethrows() {
        when(notificationService.send(any())).thenThrow(new RuntimeException("Email failed"));
        assertThrows(RuntimeException.class,
                () -> listener.handleNotificationEvent(event("PAYMENT", NotificationChannel.BOTH)));
    }
}
