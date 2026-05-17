package com.parkease.notification.mapper;

import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class NotificationMapperTest {
    private NotificationMapper mapper;
    @BeforeEach void setUp() { mapper = new NotificationMapper(); }

    @Test void toDTO_mapsAllFields() {
        LocalDateTime sentAt = LocalDateTime.now();
        Notification n = Notification.builder()
                .notificationId(1L).recipientEmail("u@t.com")
                .type(NotificationType.PAYMENT).channel(NotificationChannel.BOTH)
                .title("Payment OK").message("Paid 200").relatedId(42L)
                .relatedType("PAYMENT").isRead(true).sentAt(sentAt).readAt(sentAt).build();
        NotificationResponseDTO dto = mapper.toDTO(n);
        assertEquals(1L, dto.getNotificationId());
        assertEquals("u@t.com", dto.getRecipientEmail());
        assertEquals(NotificationType.PAYMENT, dto.getType());
        assertEquals("Payment OK", dto.getTitle());
        assertTrue(dto.isRead());
        assertEquals(sentAt, dto.getSentAt());
    }

    @Test void toDTO_unreadNotification_isReadFalse() {
        Notification n = Notification.builder()
                .notificationId(2L).recipientEmail("u@t.com")
                .type(NotificationType.CHECKIN).channel(NotificationChannel.IN_APP)
                .title("Checked In").message("Welcome").isRead(false)
                .sentAt(LocalDateTime.now()).build();
        assertFalse(mapper.toDTO(n).isRead());
        assertNull(mapper.toDTO(n).getReadAt());
    }
}
